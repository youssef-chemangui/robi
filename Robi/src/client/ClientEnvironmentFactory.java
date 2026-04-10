package client;

import exercice5.*;
import graphicLayer.*;

import java.awt.Dimension;

/**
 * Construit l'environnement côté client.
 * Identique au serveur mais avec sa propre fenêtre GSpace.
 */
public class ClientEnvironmentFactory {

    public static Environment create() {
        GSpace space = new GSpace("Client — rendu local", new Dimension(300, 200));
        space.open();

        Reference spaceRef      = new Reference(space);
        Reference rectClassRef  = new Reference(GRect.class);
        Reference ovalClassRef  = new Reference(GOval.class);
        Reference imageClassRef = new Reference(GImage.class);
        Reference stringClassRef= new Reference(GString.class);

        spaceRef.addCommand("setColor",  new SetColor());
        spaceRef.addCommand("sleep",     new Sleep());
        spaceRef.addCommand("add",       new AddElement());
        spaceRef.addCommand("del",       new DelElement());
        spaceRef.addCommand("setDim",    new SetDim());
        spaceRef.addCommand("translate", new Translate());

        rectClassRef.addCommand("new",   new NewElement());
        ovalClassRef.addCommand("new",   new NewElement());
        imageClassRef.addCommand("new",  new NewImage());
        stringClassRef.addCommand("new", new NewString());

        Environment env = new Environment();
        env.addReference("space",  spaceRef);
        env.addReference("Rect",   rectClassRef);
        env.addReference("Oval",   ovalClassRef);
        env.addReference("Image",  imageClassRef);
        env.addReference("Label",  stringClassRef);

        EnvironmentHolder.setInstance(env);
        return env;
    }
}
