package client;

import exercice5.*;
import serveur.ServerProtocol;
import serveur.SNodeSerializer;
import stree.parser.SNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

/**
 * Cette classe représente l’interface graphique principale du client.
 *
 * Son rôle est de :
 * - demander les informations de connexion au démarrage ;
 * - afficher la fenêtre principale du client ;
 * - permettre à l’utilisateur d’envoyer des S-expressions au serveur ;
 * - recevoir les réponses du serveur et mettre à jour l’affichage local ;
 * - proposer des actions rapides via des boutons.
 *
 * Cette classe gère donc à la fois :
 * - une partie interface graphique (Swing) ;
 * - une partie réseau (socket, envoi/réception) ;
 * - une partie interaction avec l’environnement local du client.
 */
public class ClientIHM {

    // ============================
    // Données réseau
    // ============================

    // Adresse du serveur auquel le client va essayer de se connecter
    private String       host       = "localhost";

    // Port du serveur
    private int          port       = 4444;

    // Nom choisi pour identifier ce client côté serveur
    private String       clientName = "Client1";

    // Socket utilisée pour la communication réseau avec le serveur
    private Socket         socket;

    // Flux de sortie pour envoyer des commandes au serveur
    private PrintWriter    out;

    // Flux d’entrée pour lire les réponses envoyées par le serveur
    private BufferedReader serverIn;

    // Indique si le client est actuellement connecté
    private boolean        connected = false;

    // ============================
    // Environnement local du client
    // ============================

    // Environnement utilisé côté client pour rejouer les commandes reçues
    private final Environment clientEnv;

    // ============================
    // Composants graphiques principaux
    // ============================

    // Fenêtre principale
    private JFrame     frame;

    // Zone d’affichage des messages et réponses
    private JTextArea  logArea;

    // Champ de saisie pour entrer une S-expression
    private JTextField inputField;

    // Libellé affichant l’état de connexion
    private JLabel     statusLabel;

    // Bouton permettant de se déconnecter du serveur
    private JButton    disconnectBtn; // MODIF 3 : bouton déconnexion

    // ============================
    // Historique des commandes tapées
    // ============================

    // Liste des commandes envoyées précédemment
    private final List<String> history    = new ArrayList<>();

    // Indice utilisé pour naviguer dans l’historique avec les flèches
    private int                historyIdx = -1;

    /**
     * Constructeur de l’interface.
     *
     * Il crée l’environnement local du client
     * puis ouvre directement la boîte de dialogue de connexion.
     */
    public ClientIHM() {
        clientEnv = ClientEnvironmentFactory.create();
        showLoginDialog();   // étape 1 : connexion
    }

    // =========================================================
    //  Étape 1 : dialogue de connexion
    // =========================================================

    /**
     * Cette méthode affiche une boîte de dialogue modale de connexion.
     *
     * L’utilisateur doit saisir :
     * - son nom de client ;
     * - l’hôte du serveur ;
     * - le port du serveur.
     *
     * Si les champs sont valides, la fenêtre principale est construite.
     */
    private void showLoginDialog() {
        JDialog dialog = new JDialog((Frame) null, "Connexion au serveur", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(360, 300); // légèrement agrandi pour le bouton
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Titre de la boîte de dialogue
        JLabel title = new JLabel("Connexion au serveur");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        root.add(title, BorderLayout.NORTH);

        // Panneau contenant les champs du formulaire
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 10));

        JTextField nameField = new JTextField(clientName);
        JTextField hostField = new JTextField(host);
        JTextField portField = new JTextField(String.valueOf(port));

        form.add(new JLabel("Nom du client :"));
        form.add(nameField);
        form.add(new JLabel("Hôte :"));
        form.add(hostField);
        form.add(new JLabel("Port :"));
        form.add(portField);

        root.add(form, BorderLayout.CENTER);

        // Partie basse avec message d’erreur et boutons
        JPanel southPanel = new JPanel(new BorderLayout(4, 6));

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(errorLabel.getFont().deriveFont(12f));
        southPanel.add(errorLabel, BorderLayout.NORTH);

        // MODIF 1 : bouton "Se connecter" explicite + bouton Annuler
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelBtn = new JButton("Annuler");
        JButton okBtn     = new JButton("Se connecter");   // ← bouton visible
        okBtn.setFont(okBtn.getFont().deriveFont(Font.BOLD));
        btnPanel.add(cancelBtn);
        btnPanel.add(okBtn);
        southPanel.add(btnPanel, BorderLayout.SOUTH);

        root.add(southPanel, BorderLayout.SOUTH);

        // Si l’utilisateur annule, on ferme complètement l’application
        cancelBtn.addActionListener(e -> System.exit(0));

        /**
         * Cette action vérifie les données saisies puis,
         * si elles sont correctes, ferme le dialogue
         * et construit l’interface principale.
         */
        Runnable tryLogin = () -> {
            String name = nameField.getText().trim();
            String h    = hostField.getText().trim();
            String p    = portField.getText().trim();

            // Vérification du nom du client
            if (name.isEmpty()) { errorLabel.setText("Le nom du client ne peut pas être vide."); return; }

            // Vérification de l’hôte
            if (h.isEmpty())    { errorLabel.setText("L'hôte ne peut pas être vide."); return; }

            int parsedPort;
            try {
                // Vérification que le port est bien un entier valide
                parsedPort = Integer.parseInt(p);
                if (parsedPort < 1 || parsedPort > 65535) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Port invalide (1–65535).");
                return;
            }

            // Mise à jour des informations de connexion
            clientName = name;
            host       = h;
            port       = parsedPort;

            // Fermeture du dialogue puis ouverture de la fenêtre principale
            dialog.dispose();
            buildUI();   // étape 2 : ouvrir la fenêtre principale
        };

        // Le bouton "Se connecter" déclenche la même action
        okBtn.addActionListener(e -> tryLogin.run());

        // Entrée dans n'importe quel champ = valider (conservé en plus du bouton)
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) tryLogin.run();
            }
        };
        nameField.addKeyListener(enter);
        hostField.addKeyListener(enter);
        portField.addKeyListener(enter);

        dialog.add(root);
        dialog.setVisible(true);
    }

    // =========================================================
    //  Étape 2 : fenêtre principale
    // =========================================================

    /**
     * Cette méthode construit toute la fenêtre principale du client.
     *
     * Elle ajoute :
     * - une barre de menu ;
     * - une barre d’outils ;
     * - le panneau central ;
     * - une barre de statut.
     *
     * Ensuite, elle tente directement la connexion au serveur.
     */
    private void buildUI() {
        frame = new JFrame("Client — " + clientName + "  [" + host + ":" + port + "]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(780, 600);
        frame.setMinimumSize(new Dimension(600, 400));
        frame.setLayout(new BorderLayout(4, 4));

        frame.setJMenuBar(buildMenuBar());
        frame.add(buildToolBar(),     BorderLayout.NORTH);
        frame.add(buildCenterPanel(), BorderLayout.CENTER);
        frame.add(buildStatusBar(),   BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        tryConnect();
    }

    // ---- Menu ----

    /**
     * Construit la barre de menus de l’application.
     *
     * Menus disponibles :
     * - Fichier
     * - Affichage
     * - Serveur
     * - Aide
     */
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu menuFichier = new JMenu("Fichier");
        menuFichier.setMnemonic('F');
        JMenuItem quit = new JMenuItem("Quitter", KeyEvent.VK_Q);
        quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        quit.addActionListener(e -> System.exit(0));
        menuFichier.add(quit);

        JMenu menuAffichage = new JMenu("Affichage");
        JMenuItem clearLog = new JMenuItem("Effacer le log");
        clearLog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        clearLog.addActionListener(e -> logArea.setText(""));
        menuAffichage.add(clearLog);

        JMenu menuServeur = new JMenu("Serveur");
        JMenuItem capture = new JMenuItem("Capture d'écran");
        capture.addActionListener(e -> requestScreenshot());
        JMenuItem save = new JMenuItem("Sauvegarder un élément");
        save.addActionListener(e -> requestSave());
        JMenuItem load = new JMenuItem("Charger un élément");
        load.addActionListener(e -> requestLoad());
        menuServeur.add(capture);
        menuServeur.add(save);
        menuServeur.add(load);

        JMenu menuAide = new JMenu("Aide");
        JMenuItem guide = new JMenuItem("Guide utilisateur");
        guide.addActionListener(e -> showUserGuide());
        menuAide.add(guide);

        bar.add(menuFichier);
        bar.add(menuAffichage);
        bar.add(menuServeur);
        bar.add(menuAide);
        return bar;
    }

    // ---- Barre d'outils — MODIF 3 : bouton Déconnecter ----

    /**
     * Construit la barre d’outils.
     *
     * Elle contient :
     * - un bouton de déconnexion ;
     * - des boutons rapides pour envoyer des commandes fréquentes ;
     * - des actions liées au serveur (capture, sauvegarde, chargement).
     */
    private JToolBar buildToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(new EmptyBorder(4, 8, 4, 8));

        // MODIF 3 : remplace "Reconnecter" par "Déconnecter"
        disconnectBtn = new JButton("Déconnecter");
        disconnectBtn.setToolTipText("Se déconnecter du serveur " + host + ":" + port);
        disconnectBtn.setEnabled(false); // activé seulement quand connecté
        disconnectBtn.addActionListener(e -> disconnect());
        tb.add(disconnectBtn);

        tb.addSeparator();

        // Boutons d’actions rapides qui envoient directement une S-expression
        addQuickButton(tb, "+ Rect",     "(space add r (Rect new))",            "Ajouter un rectangle");
        addQuickButton(tb, "+ Ovale",    "(space add o (Oval new))",            "Ajouter un ovale");
        addQuickButton(tb, "+ Texte",    "(space add t (Label new Hello))",     "Ajouter un texte");
        addQuickButton(tb, "Noir",       "(space setColor black)",              "Fond noir");
        addQuickButton(tb, "Blanc",      "(space setColor white)",              "Fond blanc");

        tb.addSeparator();

        // Boutons d’actions serveur
        addToolActionButton(tb, "Capture", "Demander une capture PNG du rendu serveur", this::requestScreenshot);
        addToolActionButton(tb, "Sauver", "Sauvegarder un élément du serveur au format JSON", this::requestSave);
        addToolActionButton(tb, "Charger", "Recharger un élément JSON côté serveur", this::requestLoad);

        return tb;
    }

    /**
     * Ajoute un bouton rapide dans la barre d’outils.
     *
     * Quand l’utilisateur clique dessus, une expression prédéfinie est envoyée.
     */
    private void addQuickButton(JToolBar tb, String label, String expr, String tooltip) {
        JButton btn = new JButton(label);
        btn.setToolTipText(tooltip + "  →  " + expr);
        btn.addActionListener(e -> sendExpression(expr));
        tb.add(btn);
    }

    /**
     * Ajoute un bouton d’action dans la barre d’outils.
     *
     * Ici, on ne passe pas une expression en texte,
     * mais directement une action à exécuter.
     */
    private void addToolActionButton(JToolBar tb, String label, String tooltip, Runnable action) {
        JButton btn = new JButton(label);
        btn.setToolTipText(tooltip);
        btn.addActionListener(e -> action.run());
        tb.add(btn);
    }

    // ---- Panneau central ----

    /**
     * Construit la partie centrale de la fenêtre.
     *
     * On y place :
     * - à gauche, le panneau d’actions ;
     * - au centre, la zone de log et la zone de saisie.
     */
    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.setBorder(new EmptyBorder(0, 8, 0, 8));

        center.add(buildActionsPanel(), BorderLayout.WEST);
        center.add(buildLogAndInput(),  BorderLayout.CENTER);

        return center;
    }

    // ---- Panneau actions (gauche) ----

    /**
     * Crée le panneau d’actions latéral.
     *
     * Ce panneau regroupe plusieurs commandes prêtes à l’emploi :
     * - changement de couleur ;
     * - suppression ;
     * - déplacement.
     */
    private JPanel buildActionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Actions"));
        panel.setPreferredSize(new Dimension(170, 0));

        // --- Couleur de robi ---
        panel.add(sectionLabel("Couleur de robi"));
        String[] robiColors = {"black","white","red","green","blue","yellow","orange","gray"};
        for (String c : robiColors) {
            addActionButton(panel, c, "(space.r setColor " + c + ")");
        }

        panel.add(Box.createVerticalStrut(10));

        // --- Suppression de robi ---
        panel.add(sectionLabel("Supprimer robi"));
        addActionButton(panel, "Supprimer robi", "(space del r)");

        panel.add(Box.createVerticalStrut(10));

        // --- Déplacer robi ---
        panel.add(sectionLabel("Déplacer robi"));

        /**
         * Ici on utilise une petite grille 3x3
         * pour simuler les flèches de déplacement.
         */
        JPanel arrows = new JPanel(new GridLayout(3, 3, 2, 2));
        arrows.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        arrows.add(new JLabel());
        addArrowBtn(arrows, "▲", "(space.r translate 0 -10)");
        arrows.add(new JLabel());
        addArrowBtn(arrows, "◀", "(space.r translate -10 0)");
        arrows.add(new JLabel());
        addArrowBtn(arrows, "▶", "(space.r translate 10 0)");
        arrows.add(new JLabel());
        addArrowBtn(arrows, "▼", "(space.r translate 0 10)");
        arrows.add(new JLabel());
        panel.add(arrows);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    /**
     * Crée un petit titre de section dans le panneau d’actions.
     */
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(6, 2, 2, 2));
        return l;
    }

    /**
     * Ajoute un bouton standard qui envoie une expression.
     */
    private void addActionButton(JPanel panel, String label, String expr) {
        JButton b = new JButton(label);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        b.setToolTipText(expr);
        b.addActionListener(e -> sendExpression(expr));
        panel.add(b);
    }

    /**
     * Ajoute un bouton de direction dans le panneau des flèches.
     */
    private void addArrowBtn(JPanel panel, String label, String expr) {
        JButton b = new JButton(label);
        b.addActionListener(e -> sendExpression(expr));
        panel.add(b);
    }

    /**
     * Variante compacte d’un bouton d’action.
     *
     * Cette méthode n’est pas utilisée ici,
     * mais elle peut servir si on veut ajouter
     * de petits boutons dans une future version.
     */
    private void addMiniActionButton(JPanel panel, String label, String expr) {
        JButton b = new JButton(label);
        b.setMargin(new Insets(2, 6, 2, 6));
        b.addActionListener(e -> sendExpression(expr));
        panel.add(b);
    }

    // ---- Log + champ de saisie ----

    /**
     * Construit la partie droite principale :
     * - la zone de log ;
     * - le champ de saisie ;
     * - le bouton d’envoi.
     *
     * Cette méthode gère aussi la navigation dans l’historique
     * avec les touches flèche haut / flèche bas.
     */
    private JPanel buildLogAndInput() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        // Zone de log non modifiable
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(220, 220, 220));
        logArea.setCaretColor(Color.WHITE);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Log — " + clientName));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
        inputPanel.setBorder(new EmptyBorder(4, 0, 4, 0));

        inputField = new JTextField();
        inputField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        inputField.setToolTipText("Entrez une S-Expression et appuyez sur Entrée");

        JButton sendBtn = new JButton("Envoyer ↵");
        sendBtn.setToolTipText("Envoyer la commande (Entrée)");

        inputPanel.add(new JLabel("  > "), BorderLayout.WEST);
        inputPanel.add(inputField,          BorderLayout.CENTER);
        inputPanel.add(sendBtn,             BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        // Entrée dans le champ = envoi
        inputField.addActionListener(e -> sendFromField());

        // Clic sur le bouton = envoi
        sendBtn.addActionListener(e -> sendFromField());

        /**
         * Gestion de l’historique :
         * - flèche haut : remonter dans les anciennes commandes ;
         * - flèche bas : redescendre.
         */
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (!history.isEmpty() && historyIdx < history.size() - 1) {
                        historyIdx++;
                        inputField.setText(history.get(history.size() - 1 - historyIdx));
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (historyIdx > 0) {
                        historyIdx--;
                        inputField.setText(history.get(history.size() - 1 - historyIdx));
                    } else {
                        historyIdx = -1;
                        inputField.setText("");
                    }
                }
            }
        });

        return panel;
    }

    // ---- Barre de statut ----

    /**
     * Construit la barre de statut placée en bas de la fenêtre.
     *
     * Elle affiche :
     * - l’état de connexion ;
     * - les informations du client (nom, hôte, port).
     */
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            new EmptyBorder(3, 8, 3, 8)
        ));
        statusLabel = new JLabel("● Déconnecté");
        statusLabel.setForeground(Color.RED);
        bar.add(statusLabel, BorderLayout.WEST);

        JLabel info = new JLabel(clientName + "  @  " + host + ":" + port);
        info.setForeground(Color.GRAY);
        bar.add(info, BorderLayout.EAST);
        return bar;
    }

    // =========================================================
    //  Réseau
    // =========================================================

    /**
     * Essaie d’ouvrir la connexion au serveur.
     *
     * Si la connexion réussit :
     * - les flux sont initialisés ;
     * - le statut passe à connecté ;
     * - un thread d’écoute du serveur est lancé.
     *
     * Si la connexion échoue, un message d’erreur est affiché dans le log.
     */
    private void tryConnect() {
        if (connected) return;
        try {
            socket   = new Socket(host, port);
            out      = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            setStatus(true);
            disconnectBtn.setEnabled(true); // MODIF 3 : activer le bouton déconnexion

            // Envoi d’un identifiant au serveur pour reconnaître ce client
            out.println("__CLIENT_ID__:" + clientName);

            log("[Connecté au serveur " + host + ":" + port + " en tant que « " + clientName + " »]", new Color(100, 220, 100));

            // Thread séparé pour écouter les réponses du serveur sans bloquer l’interface
            new Thread(this::listenServer, "server-listener-" + clientName).start();

        } catch (ConnectException e) {
            log("[Erreur] Serveur non disponible sur " + host + ":" + port + ". Lancez Server.java d'abord.", Color.RED);
        } catch (IOException e) {
            log("[Erreur] " + e.getMessage(), Color.RED);
        }
    }

    /**
     * Ferme proprement la connexion avec le serveur.
     *
     * Cette méthode :
     * - ferme la socket ;
     * - met à jour le statut ;
     * - ferme la fenêtre principale ;
     * - réaffiche la boîte de connexion.
     */
    private void disconnect() {
        if (!connected) return;

        try {
            connected = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ex) {
            log("[Erreur] Problème lors de la déconnexion : " + ex.getMessage(), Color.RED);
        }

        setStatus(false);
        disconnectBtn.setEnabled(false);

        log("[Déconnecté volontairement du serveur]", Color.ORANGE);

        // Fermeture de la fenêtre principale
        if (frame != null) {
            frame.dispose();
        }

        // Retour à l’écran de connexion
        SwingUtilities.invokeLater(this::showLoginDialog);
    }

    /**
     * Boucle d’écoute des messages envoyés par le serveur.
     *
     * Cette méthode tourne dans un thread séparé.
     * Chaque ligne reçue est transmise à handleResponse.
     */
    private void listenServer() {
        try {
            String json;
            while ((json = serverIn.readLine()) != null) {
                final String j = json;
                SwingUtilities.invokeLater(() -> handleResponse(j));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                // Si la déconnexion n’était pas volontaire,
                // on signale que le serveur est tombé
                if (connected) {
                    connected = false;
                    setStatus(false);
                    disconnectBtn.setEnabled(false);
                    log("[Serveur déconnecté]", Color.ORANGE);
                }
            });
        }
    }

    /**
     * Traite une réponse reçue du serveur.
     *
     * Cas possibles :
     * - message d’information ;
     * - capture d’écran ;
     * - erreur JSON ;
     * - liste de commandes à exécuter localement.
     */
    private void handleResponse(String json) {
        if (json.startsWith(ServerProtocol.INFO_PREFIX)) {
            log("[Info] " + ServerProtocol.parseInfo(json), new Color(120, 190, 255));
            return;
        }

        if (json.startsWith(ServerProtocol.SCREENSHOT_PREFIX)) {
            handleScreenshot(json);
            return;
        }

        log("[JSON] " + json, Color.GRAY);

        if (json.startsWith("{\"error\"")) {
            log("[Erreur serveur] " + json, Color.RED);
            return;
        }

        try {
            // Reconstruction des nœuds reçus depuis le JSON
            List<SNode> nodes = SNodeSerializer.fromJson(json);

            // Exécution locale de chaque commande reçue
            for (SNode node : nodes) {
                new exercice5.Interpreter().compute(clientEnv, node);
            }

            // Récupération de l’espace graphique pour le rafraîchir
            Reference spaceRef = clientEnv.getReferenceByName("space");
            graphicLayer.GSpace space = (graphicLayer.GSpace) spaceRef.getReceiver();
            space.repaint();   // redessiner la fenêtre

            log("[OK] Exécuté côté client", new Color(100, 200, 100));
        } catch (Exception e) {
            log("[Erreur client] " + e.getMessage(), Color.RED);
        }
    }

    // =========================================================
    //  Envoi de commandes
    // =========================================================

    /**
     * Lit le texte du champ de saisie puis l’envoie si ce texte n’est pas vide.
     *
     * Cette méthode remet aussi l’historique courant à la position initiale.
     */
    private void sendFromField() {
        String expr = inputField.getText().trim();
        if (expr.isEmpty()) return;
        inputField.setText("");
        historyIdx = -1;
        sendExpression(expr);
    }

    /**
     * Envoie une expression au serveur.
     *
     * Cette méthode :
     * - vérifie d’abord que le client est connecté ;
     * - ajoute la commande à l’historique ;
     * - l’affiche dans le log ;
     * - l’envoie au serveur.
     */
    private void sendExpression(String expr) {
        if (!connected) {
            log("[Non connecté] " + expr, Color.ORANGE);
            return;
        }

        // On évite de stocker deux fois de suite exactement la même commande
        if (history.isEmpty() || !history.get(history.size()-1).equals(expr)) {
            history.add(expr);
            if (history.size() > 50) history.remove(0);
        }

        log("> " + expr, Color.WHITE);
        out.println(expr);
    }

    /**
     * Demande au serveur une capture d’écran du rendu.
     */
    private void requestScreenshot() {
        sendProtocolLine(ServerProtocol.CAPTURE_REQUEST, "[Capture demandée au serveur]");
    }

    /**
     * Ouvre une boîte de dialogue pour demander la sauvegarde d’un élément.
     *
     * L’utilisateur choisit :
     * - le chemin de l’élément ;
     * - le nom du fichier de sauvegarde.
     */
    private void requestSave() {
        JTextField pathField = new JTextField("space.r");
        JTextField nameField = new JTextField("robi");

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Chemin de l'élément (ex: space.r ou space.robi) :"));
        panel.add(pathField);
        panel.add(new JLabel("Nom du fichier de sauvegarde :"));
        panel.add(nameField);

        int choice = JOptionPane.showConfirmDialog(
            frame, panel, "Sauvegarder un élément", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String dottedPath = pathField.getText().trim();
        String saveName = nameField.getText().trim();

        if (dottedPath.isEmpty() || saveName.isEmpty()) {
            log("[Sauvegarde annulée] chemin ou nom vide", Color.ORANGE);
            return;
        }

        sendProtocolLine(
            ServerProtocol.buildSaveRequest(dottedPath, saveName),
            "[Sauvegarde demandée] " + dottedPath + " -> " + saveName + ".json"
        );
    }

    /**
     * Demande le chargement d’un fichier JSON sauvegardé côté serveur.
     */
    private void requestLoad() {
        String saveName = JOptionPane.showInputDialog(
            frame,
            "Nom du fichier à charger (sans .json si vous voulez) :",
            "Charger un élément",
            JOptionPane.PLAIN_MESSAGE
        );
        if (saveName == null) {
            return;
        }
        saveName = saveName.trim();
        if (saveName.isEmpty()) {
            log("[Chargement annulé] nom vide", Color.ORANGE);
            return;
        }

        sendProtocolLine(
            ServerProtocol.buildLoadRequest(saveName),
            "[Chargement demandé] " + saveName + ".json"
        );
    }

    /**
     * Envoie une ligne du protocole interne au serveur
     * et ajoute un message explicatif dans le log.
     */
    private void sendProtocolLine(String wireLine, String logLine) {
        if (!connected) {
            log("[Non connecté] " + logLine, Color.ORANGE);
            return;
        }
        log(logLine, Color.WHITE);
        out.println(wireLine);
    }

    /**
     * Traite une capture d’écran envoyée par le serveur.
     *
     * Le contenu reçu est décodé en image PNG,
     * puis affiché dans une fenêtre séparée.
     */
    private void handleScreenshot(String line) {
        try {
            ServerProtocol.ScreenshotPayload payload = ServerProtocol.parseScreenshot(line);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(payload.pngBytes));
            if (image == null) {
                throw new IOException("PNG invalide");
            }
            log(
                "[Capture reçue] " + payload.fileName + " (" + image.getWidth() + "x" + image.getHeight() + ")",
                new Color(120, 190, 255)
            );
            showScreenshotDialog(payload.fileName, image);
        } catch (Exception e) {
            log("[Erreur capture] " + e.getMessage(), Color.RED);
        }
    }

    /**
     * Affiche l’image reçue dans une boîte de dialogue.
     *
     * Un JScrollPane est utilisé pour pouvoir visualiser
     * de grandes captures sans dépasser la taille de la fenêtre.
     */
    private void showScreenshotDialog(String fileName, BufferedImage image) {
        JLabel label = new JLabel(new ImageIcon(image));
        JScrollPane scroll = new JScrollPane(label);
        scroll.setPreferredSize(new Dimension(
            Math.min(720, image.getWidth() + 24),
            Math.min(520, image.getHeight() + 24)
        ));

        JDialog dialog = new JDialog(frame, "Capture serveur — " + fileName, false);
        dialog.getContentPane().add(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    // =========================================================
    //  Utilitaires UI
    // =========================================================

    /**
     * Ajoute un message à la zone de log.
     *
     * L’ajout est fait dans le thread Swing
     * pour éviter les problèmes d’accès concurrent.
     *
     * Remarque :
     * ici la couleur est passée en paramètre,
     * mais dans cette version elle n’est pas appliquée visuellement
     * car JTextArea ne gère pas facilement plusieurs couleurs de texte.
     */
    private void log(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Met à jour le texte et la couleur du statut de connexion.
     */
    private void setStatus(boolean ok) {
        statusLabel.setText(ok ? "● Connecté" : "● Déconnecté");
        statusLabel.setForeground(ok ? new Color(0, 180, 0) : Color.RED);
    }

    /**
     * Affiche un guide utilisateur dans une boîte de dialogue.
     *
     * Ce guide résume les principales fonctionnalités du client.
     */
    private void showUserGuide() {
        String guide =
            "=== Guide utilisateur ===\n\n" +
            "1. CONNEXION\n" +
            "   Au démarrage, un dialogue vous demande votre nom,\n" +
            "   l'hôte et le port du serveur.\n" +
            "   Cliquez sur « Se connecter » ou appuyez sur Entrée.\n" +
            "   Lancez Server.java AVANT de vous connecter.\n" +
            "   Vous pouvez lancer plusieurs clients avec des noms différents.\n\n" +
            "2. SAISIE DE COMMANDES\n" +
            "   Tapez une S-Expression dans le champ en bas et appuyez sur Entrée.\n" +
            "   Exemples :\n" +
            "     (space setColor black)\n" +
            "     (space add robi (Rect new))\n" +
            "     (space.robi setColor yellow)\n" +
            "     (space.robi translate 50 30)\n\n" +
            "3. HISTORIQUE\n" +
            "   Flèche ↑ = commande précédente\n" +
            "   Flèche ↓ = commande suivante\n\n" +
            "4. PANNEAU GAUCHE\n" +
            "   Fond de l'espace, couleurs de robi, formes (Rect / Oval / Txt sur une ligne), déplacements.\n\n" +
            "5. DÉCONNEXION\n" +
            "   Utilisez le bouton « Déconnecter » dans la barre d'outils.\n\n" +
            "6. CAPTURE / SAUVEGARDE / CHARGEMENT\n" +
            "   Menu « Serveur » ou boutons Capture / Sauver / Charger.\n" +
            "   Sauvegarde : indiquez un chemin d'élément (ex: space.r).\n" +
            "   Format serveur : JSON explicite contenant le chemin et les commandes de recréation.\n\n" +
            "7. RACCOURCIS CLAVIER\n" +
            "   Entrée      = envoyer la commande\n" +
            "   Ctrl+L      = effacer le log\n" +
            "   Ctrl+Q      = quitter\n\n" +
            "8. MULTI-CLIENT\n" +
            "   Chaque instance s'identifie avec son nom sur le serveur.\n" +
            "   Les deux fenêtres GSpace (serveur + client) doivent être identiques.\n";

        JTextArea ta = new JTextArea(guide);
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(frame,
            new JScrollPane(ta),
            "Guide utilisateur",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================
    //  Point d'entrée — lancer plusieurs instances pour multi-client
    // =========================================================

    /**
     * Point d’entrée du programme.
     *
     * On lance l’interface dans le thread Swing.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientIHM::new);
    }
}