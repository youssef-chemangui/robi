package exercice6;

import java.awt.Dimension;
import java.lang.reflect.Method;
import graphicLayer.GSpace;
import stree.parser.SNode;

public class SetDim implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        int w = Integer.parseInt(method.get(2).contents());
        int h = Integer.parseInt(method.get(3).contents());
        Object obj = receiver.getReceiver();

        // GSpace est un conteneur Swing : setSize(Dimension)
        if (obj instanceof GSpace) {
            ((GSpace) obj).setSize(new Dimension(w, h));
            return receiver;
        }

        // Essai 1 : setDim(Dimension)
        if (tryInvoke(obj, "setDim", new Class[]{Dimension.class}, new Dimension(w, h)))
            return receiver;

        // Essai 2 : setDim(int, int)
        if (tryInvoke(obj, "setDim", new Class[]{int.class, int.class}, w, h))
            return receiver;

        // Essai 3 : setSize(Dimension)
        if (tryInvoke(obj, "setSize", new Class[]{Dimension.class}, new Dimension(w, h)))
            return receiver;

        // Essai 4 : setSize(int, int)
        if (tryInvoke(obj, "setSize", new Class[]{int.class, int.class}, w, h))
            return receiver;

        // Essai 5 : resize(int, int)
        if (tryInvoke(obj, "resize", new Class[]{int.class, int.class}, w, h))
            return receiver;

        // Essai 6 : setDimension(Dimension)
        if (tryInvoke(obj, "setDimension", new Class[]{Dimension.class}, new Dimension(w, h)))
            return receiver;

        // Rien n'a fonctionné : afficher les méthodes disponibles pour déboguer
        System.err.println("SetDim: aucune méthode compatible trouvée sur " + obj.getClass().getName());
        System.err.println("Méthodes candidates :");
        for (Method m : obj.getClass().getMethods()) {
            String name = m.getName().toLowerCase();
            if (name.contains("dim") || name.contains("size") || name.contains("resize")) {
                System.err.println("  " + m);
            }
        }

        return receiver;
    }

    private boolean tryInvoke(Object obj, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method m = obj.getClass().getMethod(methodName, paramTypes);
            m.invoke(obj, args);
            return true;
        } catch (NoSuchMethodException e) {
            return false; // pas cette signature, on essaie la suivante
        } catch (Exception e) {
            e.printStackTrace();
            return true; // méthode trouvée mais erreur à l'exécution
        }
    }
}