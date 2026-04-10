package exercice6;

import java.awt.Point;
import stree.parser.SNode;

public class Translate implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        int dx = Integer.parseInt(method.get(2).contents());
        int dy = Integer.parseInt(method.get(3).contents());
        try {
            receiver.getReceiver().getClass()
                .getMethod("translate", Point.class)
                .invoke(receiver.getReceiver(), new Point(dx, dy));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return receiver;
    }
}