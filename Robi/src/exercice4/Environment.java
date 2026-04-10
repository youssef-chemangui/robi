package exercice4;

import java.util.HashMap;

public class Environment {
    private HashMap<String, Reference> variables;

    public Environment() {
        variables = new HashMap<>();
    }

    public void addReference(String name, Reference ref) {
        variables.put(name, ref);
    }
    
    public Reference getReferenceByName(String name) {
        Reference ref = variables.get(name);
        if (ref == null)
            throw new Error("Unknown reference: " + name);
        return ref;
    }
    

    public void removeReference(String name) {
        variables.remove(name);
    }
}