package exercice5;

import graphicLayer.GElement;
import stree.parser.SNode;

public class DelElement implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        // (space del robi)
        String elementName = method.get(2).contents();

        // *** MODIFICATION EXERCICE 5 ***
        // L'enfant est cherché dans le localEnv du receiver, pas l'env global.
        Reference elementRef = receiver.getLocalEnv().getReferenceByName(elementName);
        GElement element = (GElement) elementRef.getReceiver();

        // Suppression graphique
        try {
            receiver.getReceiver().getClass()
                .getMethod("removeElement", GElement.class)
                .invoke(receiver.getReceiver(), element);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Suppression dans le localEnv du receiver
        // (le sous-arbre entier disparaît avec lui car localEnv de elementRef
        //  n'est plus référencé nulle part)
        receiver.getLocalEnv().removeReference(elementName);

        return receiver;
    }
}