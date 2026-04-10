package exercice6;

import graphicLayer.GElement;
import stree.parser.SNode;

public class Clear implements Command {

    @Override
    public Reference run(Reference receiver, SNode method) {

        Object obj = receiver.getReceiver();

        try {
            // 🔥 méthode générique (si dispo)
            obj.getClass().getMethod("clear").invoke(obj);
        } catch (Exception e) {

            // fallback : supprimer les éléments un par un
            try {
                var elements = (Iterable<GElement>) obj.getClass()
                        .getMethod("getElements")
                        .invoke(obj);

                for (GElement el : elements) {
                    obj.getClass()
                       .getMethod("removeElement", GElement.class)
                       .invoke(obj, el);
                }

            } catch (Exception ex) {
                throw new Error("clear not supported on this object");
            }
        }

        // 🔥 IMPORTANT : vider aussi le localEnv
        receiver.getLocalEnv().clear();

        return receiver;
    }
}