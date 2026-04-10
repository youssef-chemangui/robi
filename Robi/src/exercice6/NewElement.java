package exercice6;

import graphicLayer.GElement;
import stree.parser.SNode;

public class NewElement implements Command {
    @Override
    @SuppressWarnings("unchecked")
    public Reference run(Reference receiver, SNode method) {
        try {
            GElement e = ((Class<GElement>) receiver.getReceiver())
                .getDeclaredConstructor().newInstance();
            Reference ref = new Reference(e);
            ref.addCommand("setColor", new SetColor());
            ref.addCommand("translate", new Translate());
            ref.addCommand("setDim", new SetDim());
            ref.addCommand("add", new AddElement());
            //ref.addCommand("del", new DelElement());
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}