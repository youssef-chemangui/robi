package exercice4;

import java.awt.Dimension;
import stree.parser.SNode;

public class SetDim implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        int w = Integer.parseInt(method.get(2).contents());
        int h = Integer.parseInt(method.get(3).contents());
        try {
            receiver.getReceiver().getClass()
                .getMethod("setDim", Dimension.class)
                .invoke(receiver.getReceiver(), new Dimension(w, h));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return receiver;
    }
}