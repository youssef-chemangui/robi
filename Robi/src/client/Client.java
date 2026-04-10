package client;

import exercice5.*;
import graphicLayer.*;
import serveur.SNodeSerializer;
import stree.parser.SNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.List;

/**
 * Client avec interface Swing pour saisir les S-Expressions.
 * Remplace Tools.readKeyboard() qui ne fonctionne pas dans Eclipse.
 */
public class Client {

    private static final String HOST = "localhost";
    private static final int    PORT = 4444;

    private final Environment clientEnv;
    private PrintWriter out;

    public Client() {
        clientEnv = ClientEnvironmentFactory.create();
    }

    public void start() {
        System.out.println("Connexion au serveur " + HOST + ":" + PORT + "...");
        try {
            Socket socket     = new Socket(HOST, PORT);
            out               = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connecte !");
            buildUI(in);

        } catch (ConnectException e) {
            JOptionPane.showMessageDialog(null,
                "Impossible de se connecter au serveur.\nLancez Server.java d'abord.",
                "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void buildUI(BufferedReader in) {
        JFrame frame = new JFrame("Client S-Expression");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);
        frame.setLayout(new BorderLayout(8, 8));

        // Zone de log
        JTextArea log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(log);

        // Champ de saisie
        JTextField input = new JTextField();
        input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        // Bouton envoyer
        JButton send = new JButton("Envoyer");

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);

        frame.add(scroll, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setVisible(true);

        // Action envoi (bouton ou touche Entree)
        ActionListener sendAction = e -> {
            String expr = input.getText().trim();
            if (expr.isEmpty()) return;
            input.setText("");
            log.append("> " + expr + "\n");

            // Envoi au serveur
            out.println(expr);

            // Lecture reponse dans un thread separe pour ne pas bloquer l'UI
            new Thread(() -> {
                try {
                    String json = in.readLine();
                    if (json == null) {
                        SwingUtilities.invokeLater(() -> log.append("[Serveur deconnecte]\n"));
                        return;
                    }
                    SwingUtilities.invokeLater(() -> log.append("[JSON] " + json + "\n"));

                    if (json.startsWith("{\"error\"")) {
                        SwingUtilities.invokeLater(() -> log.append("[Erreur serveur]\n"));
                        return;
                    }

                    // Execution cote client
                    List<SNode> nodes = SNodeSerializer.fromJson(json);
                    for (SNode node : nodes) {
                        new Interpreter().compute(clientEnv, node);
                    }
                    SwingUtilities.invokeLater(() -> log.append("[OK]\n"));

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> log.append("[Erreur] " + ex.getMessage() + "\n"));
                }
            }).start();
        };

        send.addActionListener(sendAction);
        input.addActionListener(sendAction); // Entree = envoyer
    }

    public static void main(String[] args) {
        new Client().start();
    }
}