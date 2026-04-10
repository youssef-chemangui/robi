package exercice4;

import graphicLayer.GElement;
import graphicLayer.GSpace;
import stree.parser.SNode;

public class AddElement implements Command {
    @Override
    public Reference run(Reference receiver, SNode method) {
        // (space add robi (rect.class new))
        // method.get(0) = space
        // method.get(1) = add
        // method.get(2) = robi  (le nom)
        // method.get(3) = (rect.class new)  (la sous-expression)
        String elementName = method.get(2).contents();
        SNode subExpr = method.get(3);

        // On récupère l'environnement global via un singleton
        Environment env = EnvironmentHolder.getInstance();

        // On résout la sous-expression : ex. (rect.class new)
        String className = subExpr.get(0).contents();
        Reference classRef = env.getReferenceByName(className);
        Reference newElementRef = classRef.run(subExpr);

        // On ajoute l'élément graphique au receiver (GSpace ou GContainer)
        GElement element = (GElement) newElementRef.getReceiver();
        try {
            receiver.getReceiver().getClass()
                .getMethod("addElement", GElement.class)
                .invoke(receiver.getReceiver(), element);
        } catch (Exception e) {
            // Essai avec GSpace directement
            if (receiver.getReceiver() instanceof GSpace) {
                ((GSpace) receiver.getReceiver()).addElement(element);
            } else {
                e.printStackTrace();
            }
        }

        // On enregistre la nouvelle référence dans l'environnement
        env.addReference(elementName, newElementRef);
        return receiver;
    }
}