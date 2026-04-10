package exercice4;

import java.awt.Image;
import java.awt.Toolkit;

import graphicLayer.GImage;
import stree.parser.SNode;

public class NewImage implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        String filename = method.get(2).contents();

        Image raw = Toolkit.getDefaultToolkit().getImage(filename);
        GImage img = new GImage(raw);

        Reference ref = new Reference(img);
        ref.addCommand("translate", new Translate());
        return ref;
    }
}