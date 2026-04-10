package exercice4;

import graphicLayer.GElement;
import stree.parser.SNode;

public class DelElement implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        String elementName = method.get(2).contents();
        Environment env = EnvironmentHolder.getInstance();
        Reference elementRef = env.getReferenceByName(elementName);
        GElement element = (GElement) elementRef.getReceiver();
        try {
            receiver.getReceiver().getClass()
                .getMethod("removeElement", GElement.class)
                .invoke(receiver.getReceiver(), element);
        } catch (Exception e) {
            e.printStackTrace();
        }
        env.removeReference(elementName);
        return receiver;
    }
}