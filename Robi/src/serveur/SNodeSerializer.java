package serveur;

import stree.parser.SNode;
import stree.parser.SParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Sérialise et désérialise des SNodes en JSON.
 *
 * Format JSON d'un SNode :
 *  - Feuille  : { "v": "mot" }
 *  - Nœud     : { "c": [ enfant1, enfant2, ... ] }
 *
 * Exemple : (space setColor red)
 *  → { "c": [ {"v":"space"}, {"v":"setColor"}, {"v":"red"} ] }
 *
 * Liste de SNodes (réponse du serveur) :
 *  → [ node1, node2, ... ]
 */
public class SNodeSerializer {

    // -------------------------
    //  Sérialisation → JSON
    // -------------------------

    public static String toJson(List<SNode> nodes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(nodeToJson(nodes.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String nodeToJson(SNode node) {
        if (node.size() == 0) {
            // Feuille
            String v = node.contents() == null ? "" : node.contents();
            return "{\"v\":\"" + escape(v) + "\"}";
        }
        // Nœud composite
        StringBuilder sb = new StringBuilder("{\"c\":[");
        for (int i = 0; i < node.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(nodeToJson(node.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // -------------------------
    //  Désérialisation ← JSON
    // -------------------------

    /**
     * Reconstruit la liste de SNodes depuis le JSON renvoyé par le serveur.
     * On repasse par SParser en reconstruisant la S-Expression textuelle.
     * C'est la méthode la plus robuste car elle réutilise SParser côté client.
     *
     * À terme (quand SParser sera retiré du client), on parsera le JSON
     * directement avec jsonToSNode().
     */
    public static List<SNode> fromJson(String json) {
        // Reconstruction de la S-Expression depuis le JSON
        String sExpr = jsonToSExpr(json);
        try {
            SParser<SNode> parser = new SParser<>();
            return parser.parse(sExpr);
        } catch (Exception e) {
            throw new RuntimeException("Désérialisation échouée : " + e.getMessage(), e);
        }
    }

    /**
     * Convertit le JSON en S-Expression textuelle.
     * Ex: [{"c":[{"v":"space"},{"v":"setColor"},{"v":"red"}]}]
     *  →  (space setColor red)
     */
    private static String jsonToSExpr(String json) {
        json = json.trim();
        // Supprimer les crochets externes de la liste
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length() - 1).trim();
        }
        StringBuilder sb = new StringBuilder();
        parseJsonNodes(json, sb);
        return sb.toString().trim();
    }

    private static int parseJsonNodes(String json, StringBuilder out) {
        int i = 0;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '{') {
                int end = findMatchingBrace(json, i);
                String obj = json.substring(i + 1, end);
                if (obj.contains("\"c\"")) {
                    // Nœud composite
                    int arrStart = obj.indexOf('[');
                    int arrEnd   = findMatchingBracket(obj, arrStart);
                    String children = obj.substring(arrStart + 1, arrEnd);
                    out.append("(");
                    parseJsonNodes(children, out);
                    // Supprimer l'espace en trop à la fin avant )
                    if (out.length() > 0 && out.charAt(out.length()-1) == ' ')
                        out.deleteCharAt(out.length()-1);
                    out.append(") ");
                } else {
                    // Feuille {"v":"..."}
                    int vs = obj.indexOf("\"v\":\"") + 5;
                    int ve = obj.indexOf("\"", vs);
                    String val = obj.substring(vs, ve)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n");
                    out.append(val).append(" ");
                }
                i = end + 1;
                // Sauter la virgule éventuelle
                if (i < json.length() && json.charAt(i) == ',') i++;
            } else {
                i++;
            }
        }
        return i;
    }

    private static int findMatchingBrace(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') { depth--; if (depth == 0) return i; }
        }
        return s.length() - 1;
    }

    private static int findMatchingBracket(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '[') depth++;
            else if (s.charAt(i) == ']') { depth--; if (depth == 0) return i; }
        }
        return s.length() - 1;
    }
}
