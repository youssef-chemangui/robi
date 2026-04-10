package exercice5;

import java.util.LinkedHashMap;
import java.util.Map;

public class Environment {
    private Map<String, Reference> variables;

    public Environment() {
        variables = new LinkedHashMap<>();
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

    public boolean hasReference(String name) {
        return variables.containsKey(name);
    }

    public Map<String, Reference> snapshot() {
        return new LinkedHashMap<>(variables);
    }
}
