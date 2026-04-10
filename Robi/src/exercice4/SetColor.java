package exercice4;

import stree.parser.SNode;
import tools.Tools;
import java.awt.Color;

public class SetColor implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        String colorName = method.get(2).contents();
        Color color = Tools.getColorByName(colorName);
        if (color == null)
            throw new Error("Unknown color: " + colorName);
        // fonctionne pour GSpace, GRect, GOval, GString...
        try {
            receiver.getReceiver().getClass()
                .getMethod("setColor", Color.class)
                .invoke(receiver.getReceiver(), color);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return receiver;
    }
}