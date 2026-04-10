package exercice4;

import stree.parser.SNode;

public interface Command {
    public Reference run(Reference receiver, SNode method);
}