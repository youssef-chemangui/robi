package exercice6;

import java.util.List;

import stree.parser.SNode;

public class ScriptCommand implements Command {

    private SNode params;
    private List<SNode> body;

    public ScriptCommand(SNode params, List<SNode> body) {
        this.params = params;
        this.body = body;
    }

    @Override
    public Reference run(Reference receiver, SNode method) {

        Environment globalEnv = EnvironmentHolder.getInstance();
        Environment localEnv = new Environment(globalEnv);

        // self
        localEnv.addReference("self", receiver);

        // paramètres
        for (int i = 1; i < params.size(); i++) {

            String paramName = params.get(i).contents();
            String argValue  = method.get(i + 1).contents();

            localEnv.addReference(paramName, new Reference(argValue));
        }

        // 🔥 exécution correcte
        Interpreter interpreter = new Interpreter();

        for (SNode expr : body) {

            // 🔥 substitution des paramètres AVANT exécution
            substituteInPlace(expr, localEnv);

            interpreter.compute(localEnv, expr);
        }

        return receiver;
    }
    
    private void substituteInPlace(SNode node, Environment env) {

    	if (node.isLeaf()) {

    	    String value = node.contents();

    	    // 🔥 CAS 1 : nom simple (c, name, etc.)
    	    Reference ref = env.getReferenceByName(value);

    	    if (ref != null && ref.getReceiver() instanceof String) {
    	        node.setContents((String) ref.getReceiver());
    	        return;
    	    }

    	    // 🔥 CAS 2 : nom pointé (self.name)
    	    if (value.contains(".")) {

    	        String[] parts = value.split("\\.");

    	        for (int i = 0; i < parts.length; i++) {

    	            Reference partRef = env.getReferenceByName(parts[i]);

    	            if (partRef != null && partRef.getReceiver() instanceof String) {
    	                parts[i] = (String) partRef.getReceiver();
    	            }
    	        }

    	        // reconstruire
    	        String newValue = String.join(".", parts);
    	        node.setContents(newValue);
    	    }

    	    return;
    	}
        // sinon parcourir les enfants
        for (int i = 0; i < node.size(); i++) {
            substituteInPlace(node.get(i), env);
        }
    }
}