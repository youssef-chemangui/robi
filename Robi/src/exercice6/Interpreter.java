package exercice6;

import stree.parser.SNode;

public class Interpreter {

    public Reference resolve(Environment env, String dottedName) {

        String[] parts = dottedName.split("\\.");

        Reference current = env.getReferenceByName(parts[0]);

        if (current == null) {
            throw new Error("Unknown reference: " + parts[0]);
        }

        // 🔥 navigation enfants
        for (int i = 1; i < parts.length; i++) {

            current = current.getLocalEnv().getReferenceByName(parts[i]);

            if (current == null) {
                throw new Error("Unknown sub-reference: " + parts[i]);
            }
        }

        return current;
    }

    public Reference compute(Environment env, SNode expr) {

        String receiverName = expr.get(0).contents();

        Reference receiver = resolve(env, receiverName);

        return receiver.run(expr);
    }
}