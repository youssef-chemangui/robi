package exercice5;

import stree.parser.SNode;

public class Interpreter {

    /**
     * Résout un nom potentiellement pointé (ex: "space.robi.im")
     * en partant de l'environnement global et en naviguant dans
     * les sous-environnements locaux de chaque Reference.
     */
    public Reference resolve(Environment env, String dottedName) {
        String[] parts = dottedName.split("\\.");
        // Premier segment : cherché dans l'environnement global
        Reference current = env.getReferenceByName(parts[0]);
        // Segments suivants : cherchés dans le localEnv du receiver courant
        for (int i = 1; i < parts.length; i++) {
            current = current.getLocalEnv().getReferenceByName(parts[i]);
        }
        return current;
    }

    public Reference compute(Environment env, SNode expr) {
        // Le premier token est le nom du receiver (potentiellement pointé)
        String receiverName = expr.get(0).contents();
        Reference receiver = resolve(env, receiverName);
        return receiver.run(expr);
    }
}