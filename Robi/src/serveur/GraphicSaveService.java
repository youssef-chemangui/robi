package serveur;

import exercice5.Environment;
import exercice5.Interpreter;
import exercice5.Reference;
import graphicLayer.GBounded;
import graphicLayer.GElement;
import graphicLayer.GImage;
import graphicLayer.GOval;
import graphicLayer.GRect;
import graphicLayer.GString;
import stree.parser.SNode;
import stree.parser.SParser;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format de sauvegarde JSON produit par le serveur :
 * {
 *   "format": "robi-save-v1",
 *   "savedPath": "space.robi",
 *   "savedAt": "2026-04-08T12:34:56Z",
 *   "commands": [
 *     "(space add robi (Rect new))",
 *     "(space.robi setColor yellow)",
 *     "(space.robi setDim 20 20)",
 *     "(space.robi translate 10 15)"
 *   ]
 * }
 *
 * Les commandes sont suffisantes pour reconstruire l'élément lors du chargement.
 */
public class GraphicSaveService {
    private static final String FORMAT = "robi-save-v1";

    public Path save(Environment env, String dottedPath, String saveName) throws IOException {
        SaveDefinition def = buildDefinition(env, dottedPath);
        Path file = resolveSaveFile(saveName);
        Files.createDirectories(file.getParent());
        Files.write(file, def.toJson().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    public LoadResult load(Environment env, String saveName) throws IOException {
        Path file = resolveSaveFile(saveName);
        if (!Files.exists(file)) {
            throw new IOException("Save file not found: " + file.getFileName());
        }

        SaveDefinition def = SaveDefinition.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        List<String> commands = new ArrayList<>();
        if (pathExists(env, def.savedPath)) {
            commands.add(buildDeleteExpr(def.savedPath));
        }
        commands.addAll(def.commands);
        return new LoadResult(def.savedPath, file, commands, parseCommands(commands));
    }

    private SaveDefinition buildDefinition(Environment env, String dottedPath) {
        if (dottedPath == null || dottedPath.trim().isEmpty()) {
            throw new Error("Empty save path");
        }
        if (!dottedPath.contains(".")) {
            throw new Error("Use a dotted path like space.robi");
        }

        Interpreter interpreter = new Interpreter();
        Reference ref = interpreter.resolve(env, dottedPath);
        Object receiver = ref.getReceiver();
        if (!(receiver instanceof GElement)) {
            throw new Error("Only graphical elements can be saved: " + dottedPath);
        }

        List<String> commands = new ArrayList<>();
        appendElementCommands(dottedPath, ref, commands);
        return new SaveDefinition(dottedPath, commands);
    }

    private void appendElementCommands(String dottedPath, Reference ref, List<String> commands) {
        Object receiver = ref.getReceiver();
        commands.add(buildCreateExpr(dottedPath, receiver));
        appendStateCommands(dottedPath, receiver, commands);

        Map<String, Reference> children = ref.getLocalEnv().snapshot();
        List<String> childNames = new ArrayList<>(children.keySet());
        Collections.sort(childNames);
        for (String childName : childNames) {
            appendElementCommands(dottedPath + "." + childName, children.get(childName), commands);
        }
    }

    private String buildCreateExpr(String dottedPath, Object receiver) {
        String parentPath = parentPath(dottedPath);
        String elementName = leafName(dottedPath);
        String type = typeName(receiver);

        if (receiver instanceof GString) {
            String text = extractLabelText((GString) receiver);
            return "(" + parentPath + " add " + elementName + " (" + type + " new " + quote(text) + "))";
        }

        if (receiver instanceof GImage) {
            String source = extractImageSource((GImage) receiver);
            if (source == null || source.trim().isEmpty()) {
                throw new Error("Cannot save image without source path: " + dottedPath);
            }
            return "(" + parentPath + " add " + elementName + " (" + type + " new " + quote(source) + "))";
        }

        return "(" + parentPath + " add " + elementName + " (" + type + " new))";
    }

    private void appendStateCommands(String dottedPath, Object receiver, List<String> commands) {
        if (receiver instanceof GElement && !(receiver instanceof GImage)) {
            Color color = extractElementColor((GElement) receiver);
            if (color != null) {
                commands.add("(" + dottedPath + " setColor " + colorToToken(color) + ")");
            }
        }

        if (receiver instanceof GBounded && !(receiver instanceof GString)) {
            GBounded bounded = (GBounded) receiver;
            commands.add("(" + dottedPath + " setDim " + bounded.getWidth() + " " + bounded.getHeight() + ")");
        }

        Point position = extractPosition(receiver);
        if (position != null && (position.x != 0 || position.y != 0)) {
            commands.add("(" + dottedPath + " translate " + position.x + " " + position.y + ")");
        }
    }

    private Point extractPosition(Object receiver) {
        if (receiver instanceof GBounded) {
            return ((GBounded) receiver).getPosition();
        }
        if (receiver instanceof GImage) {
            return ((GImage) receiver).getPosition();
        }
        return null;
    }

    private String extractLabelText(GString label) {
        try {
            Method getter = GString.class.getMethod("getString");
            Object value = getter.invoke(label);
            return value == null ? "" : value.toString();
        } catch (Exception ignored) {
            // Fallback sur le champ historique si l'API n'est pas recompilée côté IDE.
        }

        try {
            Field field = GString.class.getDeclaredField("str");
            field.setAccessible(true);
            Object value = field.get(label);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            throw new Error("Cannot read GString contents", e);
        }
    }

    private String extractImageSource(GImage image) {
        try {
            Method getter = GImage.class.getMethod("getSourcePath");
            Object value = getter.invoke(image);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            // Fallback sur le champ si Eclipse n'a pas encore recompilé 2DGraphicCore.
        }

        try {
            Field field = GImage.class.getDeclaredField("sourcePath");
            field.setAccessible(true);
            Object value = field.get(image);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private Color extractElementColor(GElement element) {
        try {
            Method getter = GElement.class.getMethod("getColor");
            Object value = getter.invoke(element);
            return (Color) value;
        } catch (Exception ignored) {
            // Fallback sur le champ si l'API visible par Eclipse est plus ancienne.
        }

        try {
            Field field = GElement.class.getDeclaredField("color");
            field.setAccessible(true);
            return (Color) field.get(element);
        } catch (Exception e) {
            throw new Error("Cannot read element color", e);
        }
    }

    private String typeName(Object receiver) {
        if (receiver instanceof GRect) return "Rect";
        if (receiver instanceof GOval) return "Oval";
        if (receiver instanceof GImage) return "Image";
        if (receiver instanceof GString) return "Label";
        throw new Error("Unsupported element type: " + receiver.getClass().getName());
    }

    private String colorToToken(Color color) {
        try {
            for (Field field : Color.class.getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Color.class) {
                    continue;
                }
                Color candidate = (Color) field.get(null);
                if (candidate.equals(color)) {
                    return field.getName().toLowerCase();
                }
            }
        } catch (IllegalAccessException e) {
            throw new Error("Unable to inspect colors", e);
        }
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private String buildDeleteExpr(String dottedPath) {
        return "(" + parentPath(dottedPath) + " del " + leafName(dottedPath) + ")";
    }

    private boolean pathExists(Environment env, String dottedPath) {
        try {
            new Interpreter().resolve(env, dottedPath);
            return true;
        } catch (Error e) {
            return false;
        }
    }

    private List<SNode> parseCommands(List<String> commands) throws IOException {
        SParser<SNode> parser = new SParser<>();
        return parser.parse(String.join("\n", commands));
    }

    private String parentPath(String dottedPath) {
        int idx = dottedPath.lastIndexOf('.');
        if (idx <= 0) {
            throw new Error("Invalid path: " + dottedPath);
        }
        return dottedPath.substring(0, idx);
    }

    private String leafName(String dottedPath) {
        int idx = dottedPath.lastIndexOf('.');
        if (idx < 0 || idx == dottedPath.length() - 1) {
            throw new Error("Invalid path: " + dottedPath);
        }
        return dottedPath.substring(idx + 1);
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private Path resolveSaveFile(String saveName) {
        String sanitized = saveName == null ? "" : saveName.trim();
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.isEmpty()) {
            throw new Error("Invalid save name");
        }
        if (!sanitized.endsWith(".json")) {
            sanitized += ".json";
        }

        Path robiDir = Paths.get("Robi");
        Path baseDir = Files.exists(robiDir) ? robiDir : Paths.get(".");
        return baseDir.resolve("saves").resolve(sanitized).normalize();
    }

    public static final class LoadResult {
        public final String savedPath;
        public final Path file;
        public final List<String> commands;
        public final List<SNode> nodes;

        public LoadResult(String savedPath, Path file, List<String> commands, List<SNode> nodes) {
            this.savedPath = savedPath;
            this.file = file;
            this.commands = commands;
            this.nodes = nodes;
        }
    }

    private static final class SaveDefinition {
        private static final Pattern FORMAT_PATTERN =
            Pattern.compile("\"format\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");
        private static final Pattern PATH_PATTERN =
            Pattern.compile("\"savedPath\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");
        private static final Pattern COMMANDS_PATTERN =
            Pattern.compile("\"commands\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);

        final String savedPath;
        final List<String> commands;

        SaveDefinition(String savedPath, List<String> commands) {
            this.savedPath = savedPath;
            this.commands = new ArrayList<>(commands);
        }

        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"format\": \"").append(FORMAT).append("\",\n");
            sb.append("  \"savedPath\": \"").append(escape(savedPath)).append("\",\n");
            sb.append("  \"savedAt\": \"").append(Instant.now().toString()).append("\",\n");
            sb.append("  \"commands\": [\n");
            for (int i = 0; i < commands.size(); i++) {
                sb.append("    \"").append(escape(commands.get(i))).append("\"");
                if (i < commands.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}\n");
            return sb.toString();
        }

        static SaveDefinition fromJson(String json) throws IOException {
            String format = extractString(FORMAT_PATTERN, json, "format");
            if (!FORMAT.equals(unescape(format))) {
                throw new IOException("Unsupported save format");
            }

            String savedPath = unescape(extractString(PATH_PATTERN, json, "savedPath"));
            String commandsBlock = extractString(COMMANDS_PATTERN, json, "commands");
            return new SaveDefinition(savedPath, parseStringArray(commandsBlock));
        }

        private static String extractString(Pattern pattern, String json, String fieldName) throws IOException {
            Matcher matcher = pattern.matcher(json);
            if (!matcher.find()) {
                throw new IOException("Missing field: " + fieldName);
            }
            return matcher.group(1);
        }

        private static List<String> parseStringArray(String block) throws IOException {
            List<String> result = new ArrayList<>();
            int i = 0;
            while (i < block.length()) {
                while (i < block.length() && Character.isWhitespace(block.charAt(i))) i++;
                if (i < block.length() && block.charAt(i) == ',') {
                    i++;
                    continue;
                }
                if (i >= block.length()) {
                    break;
                }
                if (block.charAt(i) != '"') {
                    throw new IOException("Malformed commands array");
                }
                i++;
                StringBuilder sb = new StringBuilder();
                boolean escaped = false;
                while (i < block.length()) {
                    char c = block.charAt(i++);
                    if (escaped) {
                        switch (c) {
                            case '\\': sb.append('\\'); break;
                            case '"': sb.append('"'); break;
                            case 'n': sb.append('\n'); break;
                            case 'r': sb.append('\r'); break;
                            case 't': sb.append('\t'); break;
                            default: sb.append(c); break;
                        }
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
                result.add(sb.toString());
            }
            return result;
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");
        }

        private static String unescape(String value) {
            return value.replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
        }
    }
}
