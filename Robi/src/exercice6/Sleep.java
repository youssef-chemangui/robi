package exercice6;

import stree.parser.SNode;
import tools.Tools;

public class Sleep implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        int millis = Integer.parseInt(method.get(2).contents());
        Tools.sleep(millis);
        return receiver;
    }
}