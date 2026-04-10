package serveur;

import graphicLayer.GSpace;
import stree.parser.SNode;
import stree.parser.SParser;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cette classe représente le serveur principal du projet.
 *
 * Son rôle est de :
 * - accepter les connexions des clients ;
 * - recevoir des commandes sous forme de S-expressions ;
 * - parser ces expressions en SNode ;
 * - exécuter les commandes côté serveur ;
 * - diffuser les commandes aux clients connectés ;
 * - gérer certaines commandes spéciales du protocole
 *   comme la capture, la sauvegarde et le chargement.
 *
 * Ce serveur permet donc de synchroniser plusieurs clients
 * tout en gardant un rendu local côté serveur pour comparaison.
 */
public class Server {

    // Port fixe utilisé pour écouter les connexions clientes
    private static final int PORT = 4444;

    // Format utilisé pour générer les noms de fichiers des captures d’écran
    private static final DateTimeFormatter SCREENSHOT_TS =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    // Environnement local du serveur, utilisé pour exécuter les commandes
    // et afficher le rendu graphique de référence
    private final exercice5.Environment serverEnv;

    // Liste thread-safe des clients actuellement connectés
    private final CopyOnWriteArrayList<ClientConnection> clients = new CopyOnWriteArrayList<>();

    // Service chargé de la sauvegarde et du chargement des éléments graphiques
    private final GraphicSaveService saveService = new GraphicSaveService();

    /**
     * Constructeur du serveur.
     *
     * Il initialise l’environnement graphique local du serveur.
     */
    public Server() {
        serverEnv = ServerEnvironmentFactory.create();
    }

    /**
     * Démarre le serveur et attend les connexions des clients.
     *
     * Pour chaque client accepté, un nouveau thread est lancé
     * afin de gérer ce client indépendamment des autres.
     */
    public void start() {
        System.out.println("Serveur démarré sur le port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client connecté : " + client.getInetAddress());

                // Chaque client est géré dans son propre thread
                // pour permettre plusieurs connexions en parallèle
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gère la communication avec un client donné.
     *
     * Cette méthode :
     * - crée les flux d’entrée/sortie ;
     * - ajoute le client à la liste des clients connectés ;
     * - lit chaque ligne envoyée par le client ;
     * - transmet chaque ligne à handleLine().
     *
     * Si le client se déconnecte, il est retiré de la liste.
     */
    private void handleClient(Socket socket) {
        ClientConnection client = null;
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            client = new ClientConnection(socket, out);
            clients.add(client);

            String line;
            while ((line = in.readLine()) != null) {
                handleLine(client, line.trim());
            }
        } catch (IOException e) {
            System.out.println("Client déconnecté : " + socket.getInetAddress());
        } finally {
            if (client != null) {
                clients.remove(client);
                System.out.println("Connexion fermée : " + client.name);
            }
        }
    }

    /**
     * Traite une ligne envoyée par un client.
     *
     * Cette méthode distingue plusieurs cas :
     * - identification du client ;
     * - demande de capture d’écran ;
     * - demande de sauvegarde ;
     * - demande de chargement ;
     * - commande classique sous forme de S-expression.
     *
     * En cas d’erreur, un message JSON d’erreur est renvoyé au client.
     */
    private void handleLine(ClientConnection client, String line) {
        if (line.isEmpty()) {
            return;
        }

        try {
            // Le client envoie d’abord son identifiant pour être reconnu côté serveur
            if (line.startsWith(ServerProtocol.CLIENT_ID_PREFIX)) {
                client.name = line.substring(ServerProtocol.CLIENT_ID_PREFIX.length()).trim();
                System.out.println("Client identifié : " + client.name);
                return;
            }

            // Demande de capture d’écran du rendu serveur
            if (ServerProtocol.CAPTURE_REQUEST.equals(line)) {
                sendScreenshot(client);
                return;
            }

            // Demande de sauvegarde d’un élément graphique
            if (line.startsWith(ServerProtocol.SAVE_PREFIX)) {
                handleSave(client, ServerProtocol.parseSaveRequest(line));
                return;
            }

            // Demande de chargement d’un élément sauvegardé
            if (line.startsWith(ServerProtocol.LOAD_PREFIX)) {
                handleLoad(client, ServerProtocol.parseLoadRequest(line));
                return;
            }

            // Cas normal : on reçoit une S-expression à parser puis diffuser
            System.out.println("[Reçu][" + client.name + "] " + line);
            List<SNode> nodes = processExpression(line);
            broadcastNodes(nodes);
        } catch (Exception e) {
            client.send(ServerProtocol.buildErrorJson(e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    /**
     * Parse une S-expression, l’exécute côté serveur,
     * puis retourne les SNodes obtenus.
     *
     * Cette méthode est importante car elle joue deux rôles :
     * - transformer le texte reçu en structure exploitable ;
     * - exécuter localement la commande pour le rendu serveur.
     */
    private List<SNode> processExpression(String input) throws Exception {
        SParser<SNode> parser = new SParser<>();
        List<SNode> nodes = parser.parse(input);
        executeNodes(nodes);
        return nodes;
    }

    /**
     * Exécute une liste de nœuds SNode dans l’environnement du serveur.
     *
     * Chaque nœud correspond à une commande interprétée.
     */
    private void executeNodes(List<SNode> nodes) {
        for (SNode node : nodes) {
            new exercice5.Interpreter().compute(serverEnv, node);
        }
    }

    /**
     * Diffuse à tous les clients connectés les nœuds reçus,
     * après les avoir sérialisés au format JSON.
     *
     * Cela permet de garder tous les clients synchronisés.
     */
    private void broadcastNodes(List<SNode> nodes) {
        String json = SNodeSerializer.toJson(nodes);
        for (ClientConnection client : clients) {
            client.send(json);
        }
        System.out.println("[Diffusé] " + json);
    }

    /**
     * Gère une demande de sauvegarde envoyée par un client.
     *
     * Le service de sauvegarde enregistre l’élément demandé,
     * puis le serveur renvoie un message d’information au client.
     */
    private void handleSave(ClientConnection client, ServerProtocol.SaveRequest request) throws IOException {
        Path file = saveService.save(serverEnv, request.dottedPath, request.saveName);
        client.send(ServerProtocol.buildInfo(
            "Sauvegarde créée: " + file.getFileName() + " pour " + request.dottedPath
        ));
    }

    /**
     * Gère une demande de chargement d’un élément sauvegardé.
     *
     * Les commandes reconstruites sont :
     * - exécutées côté serveur ;
     * - diffusées à tous les clients ;
     * - puis un message d’information est renvoyé au client demandeur.
     */
    private void handleLoad(ClientConnection client, String saveName) throws Exception {
        GraphicSaveService.LoadResult result = saveService.load(serverEnv, saveName);
        executeNodes(result.nodes);
        broadcastNodes(result.nodes);
        client.send(ServerProtocol.buildInfo(
            "Chargement effectué depuis " + result.file.getFileName() + " vers " + result.savedPath
        ));
    }

    /**
     * Envoie une capture d’écran du rendu graphique du serveur à un client.
     *
     * Le contenu est encodé en PNG puis envoyé via le protocole défini.
     */
    private void sendScreenshot(ClientConnection client) throws Exception {
        byte[] pngBytes = captureServerSpace();
        String fileName = "capture-" + SCREENSHOT_TS.format(LocalDateTime.now()) + ".png";
        client.send(ServerProtocol.buildScreenshot(fileName, pngBytes));
    }

    /**
     * Capture l’état actuel de l’espace graphique du serveur en image PNG.
     *
     * Cette méthode est un peu plus technique :
     * - elle récupère l’objet GSpace ;
     * - elle calcule une taille correcte ;
     * - elle dessine le contenu dans un BufferedImage ;
     * - elle convertit ensuite l’image en tableau d’octets PNG.
     *
     * Le passage par SwingUtilities est important car le rendu graphique
     * doit être exécuté dans le thread graphique de Swing.
     */
    private byte[] captureServerSpace() throws Exception {
        final byte[][] bytesHolder = new byte[1][];
        final Exception[] failure = new Exception[1];

        Runnable task = () -> {
            try {
                GSpace space = (GSpace) serverEnv.getReferenceByName("space").getReceiver();
                Dimension preferred = space.getPreferredSize();

                // On choisit une largeur et une hauteur valides,
                // même si le composant n’a pas encore été affiché complètement
                int width = Math.max(1, space.getWidth() > 0 ? space.getWidth() : preferred.width);
                int height = Math.max(1, space.getHeight() > 0 ? space.getHeight() : preferred.height);

                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    // On force la taille puis on dessine l’espace dans l’image
                    space.setSize(width, height);
                    space.doLayout();
                    space.paint(graphics);
                } finally {
                    graphics.dispose();
                }

                // Conversion de l’image en PNG stocké en mémoire
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "png", out);
                bytesHolder[0] = out.toByteArray();
            } catch (Exception e) {
                failure[0] = e;
            }
        };

        // Si on est déjà dans le thread Swing, on exécute directement.
        // Sinon, on demande à Swing d’exécuter la tâche au bon endroit.
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeAndWait(task);
        }

        if (failure[0] != null) {
            throw failure[0];
        }
        return bytesHolder[0];
    }

    /**
     * Point d’entrée du programme serveur.
     */
    public static void main(String[] args) {
        new Server().start();
    }

    /**
     * Cette classe interne représente un client connecté au serveur.
     *
     * Elle stocke :
     * - la socket du client ;
     * - son flux de sortie ;
     * - son nom d’identification.
     *
     * Elle sert surtout à simplifier la diffusion des messages.
     */
    private static final class ClientConnection {
        private final Socket socket;
        private final PrintWriter out;
        private String name;

        /**
         * Construit un objet représentant un client connecté.
         *
         * Par défaut, un nom temporaire est attribué
         * à partir du port de la socket.
         */
        private ClientConnection(Socket socket, PrintWriter out) {
            this.socket = socket;
            this.out = out;
            this.name = "client@" + socket.getPort();
        }

        /**
         * Envoie une ligne au client.
         */
        private void send(String line) {
            out.println(line);
        }
    }
}