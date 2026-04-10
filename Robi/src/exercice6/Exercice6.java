package exercice6;




/*
 * Exercice 5 – Conteneurs imbriqués avec notation pointée
 *
 * Exemples de scripts :
 *
 *   (space setDim 150 120)
 *   (space add robi (Rect new))
 *   (space.robi setColor white)
 *   (space.robi setDim 100 100)
 *   (space.robi translate 20 10)
 *   (space.robi add im (Image new alien.gif))
 *   (space.robi.im translate 20 20)
 *
 *   (space add robi (Rect new))
 *   (space.robi setDim 50 50)
 *   (space.robi add robi (Rect new))
 *   (space.robi.robi setColor red)
 *   (space.robi setColor white)
 */

import java.awt.Dimension;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import graphicLayer.GImage;
import graphicLayer.GOval;
import graphicLayer.GRect;
import graphicLayer.GSpace;
import graphicLayer.GString;
import stree.parser.SNode;
import stree.parser.SParser;
import tools.Tools;

public class Exercice6 {

    Environment environment = new Environment();

    public Exercice6() {
        GSpace space = new GSpace("Exercice 6", new Dimension(300, 200));
        space.open();

        Reference spaceRef     = new Reference(space);
        Reference rectClassRef  = new Reference(GRect.class);
        Reference ovalClassRef  = new Reference(GOval.class);
        Reference imageClassRef = new Reference(GImage.class);
        Reference stringClassRef= new Reference(GString.class);

        spaceRef.addCommand("setColor", new SetColor());
        spaceRef.addCommand("sleep",    new Sleep());
        spaceRef.addCommand("add",      new AddElement());
        //spaceRef.addCommand("del",      new DelElement());
        spaceRef.addCommand("setDim",   new SetDim());
        spaceRef.addCommand("translate",new Translate());
        spaceRef.addCommand("addScript", new AddScript());
        spaceRef.addCommand("clear", new Clear());

        rectClassRef.addCommand("new",   new NewElement());
        ovalClassRef.addCommand("new",   new NewElement());
        imageClassRef.addCommand("new",  new NewImage());
        stringClassRef.addCommand("new", new NewString());

        environment.addReference("space", spaceRef);
        environment.addReference("Rect",  rectClassRef);
        environment.addReference("Oval",  ovalClassRef);
        environment.addReference("Image", imageClassRef);
        environment.addReference("Label", stringClassRef);

        EnvironmentHolder.setInstance(environment);

        this.mainLoop();
    }

    private void mainLoop() {
        while (true) {
            System.out.print("> ");
            String input = Tools.readKeyboard();
            SParser<SNode> parser = new SParser<>();
            List<SNode> compiled = null;
            try {
                compiled = parser.parse(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Iterator<SNode> itor = compiled.iterator();
            while (itor.hasNext()) {
                new Interpreter().compute(environment, itor.next());
            }
        }
    }

    public static void main(String[] args) {
        new Exercice6();
    }
}