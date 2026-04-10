package exercice6;

import java.util.HashMap;
import java.util.Map;
import stree.parser.SNode;

public class Reference {
    private Object receiver;
    private Map<String, Command> primitives;

    // Sous-environnement local : enfants de ce conteneur
    private Environment localEnv;
   

    public Reference(Object receiver) {
        this.receiver = receiver;
        this.primitives = new HashMap<>();
        this.localEnv = new Environment();
    }

    public Object getReceiver() {
        return receiver;
    }

    public void addCommand(String name, Command cmd) {
        primitives.put(name, cmd);
    }

    public Command getCommandByName(String name) {
        return primitives.get(name);
    }

    // Accès au sous-environnement local (enfants directs)
    public Environment getLocalEnv() {
        return localEnv;
    }

    public Reference run(SNode method) {
        String commandName = method.get(1).contents();
        Command cmd = primitives.get(commandName);
        if (cmd == null)
            throw new Error("Unknown command: " + commandName);
        return cmd.run(this, method);
    }
}