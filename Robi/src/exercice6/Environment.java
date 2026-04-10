package exercice6;

import java.util.HashMap;
import java.util.Map;

public class Environment {

    private Map<String, Reference> variables = new HashMap<>();
    private Environment parent;

    public Environment() {
        this.parent = null;
    }

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void addReference(String name, Reference ref) {
        variables.put(name, ref);
    }
    
    public void clear() {
        variables.clear();
    }

    public Reference getReferenceByName(String name) {

        Reference ref = variables.get(name);

        if (ref != null) return ref;

        if (parent != null) return parent.getReferenceByName(name);

        return null;
    }
}