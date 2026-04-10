package exercice6;

import graphicLayer.GElement;
import graphicLayer.GSpace;
import stree.parser.SNode;

public class AddElement implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        // (space add robi (Rect new))
        // method.get(0) = receiver name
        // method.get(1) = "add"
        // method.get(2) = nom de l'enfant (ex: "robi")
        // method.get(3) = sous-expression de création (ex: (Rect new))
    	String elementName = method.get(2).contents();

        // 🔥 NOUVEAU : vérifier si c’est un paramètre (ex: name)
        Reference nameRef = receiver.getLocalEnv().getReferenceByName(elementName);

        if (nameRef != null && nameRef.getReceiver() instanceof String) {
            elementName = (String) nameRef.getReceiver();
        }

        SNode subExpr = method.get(3);

        // Résolution de la classe/référence pour la sous-expression
        Environment globalEnv = EnvironmentHolder.getInstance();
        String className = subExpr.get(0).contents();
        Reference classRef = globalEnv.getReferenceByName(className);

        // Création du nouvel élément via la commande "new"
        Reference newElementRef = classRef.run(subExpr);

        // Ajout graphique dans le receiver (GSpace, GRect, GOval…)
        GElement element = (GElement) newElementRef.getReceiver();
        try {
            receiver.getReceiver().getClass()
                .getMethod("addElement", GElement.class)
                .invoke(receiver.getReceiver(), element);
        } catch (Exception e) {
            if (receiver.getReceiver() instanceof GSpace) {
                ((GSpace) receiver.getReceiver()).addElement(element);
            } else {
                e.printStackTrace();
            }
        }

        // *** MODIFICATION EXERCICE 5 ***
        // On enregistre l'enfant dans le sous-environnement LOCAL du receiver,
        // pas dans l'environnement global. Ainsi "space.robi" est résolu en
        // cherchant "robi" dans le localEnv de "space".
        receiver.getLocalEnv().addReference(elementName, newElementRef);

        return receiver;
    }
}