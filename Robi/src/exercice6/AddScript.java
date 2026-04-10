package exercice6;

import java.util.ArrayList;
import java.util.List;

import stree.parser.SNode;

public class AddScript implements Command {

    @Override
    public Reference run(Reference receiver, SNode method) {

        String scriptName = method.get(2).contents();

        SNode scriptDef = method.get(3);

        SNode paramsNode = scriptDef.get(0);
     // 🔥 body = TOUT sauf les params
        List<SNode> bodyList = new ArrayList<>();

        for (int i = 1; i < scriptDef.size(); i++) {
            bodyList.add(scriptDef.get(i));
        }

        Command script = new ScriptCommand(paramsNode, bodyList);
        receiver.addCommand(scriptName, script);

        return receiver;
    }
}