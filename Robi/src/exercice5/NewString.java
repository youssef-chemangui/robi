package exercice5;

import graphicLayer.GString;
import stree.parser.SNode;

public class NewString implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        String text = method.get(2).contents();
        GString gs = new GString(text);

        Reference ref = new Reference(gs);
        ref.addCommand("setColor", new SetColor());
        ref.addCommand("translate", new Translate());

        return ref;
    }
}