package serveur;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ServerProtocol {
    public static final String CLIENT_ID_PREFIX = "__CLIENT_ID__:";
    public static final String CAPTURE_REQUEST = "__CAPTURE__";
    public static final String SAVE_PREFIX = "__SAVE__:";
    public static final String LOAD_PREFIX = "__LOAD__:";
    public static final String INFO_PREFIX = "__INFO__:";
    public static final String SCREENSHOT_PREFIX = "__SCREENSHOT__:";

    private ServerProtocol() {
    }

    public static String buildSaveRequest(String dottedPath, String saveName) {
        return SAVE_PREFIX + encode(dottedPath) + ":" + encode(saveName);
    }

    public static SaveRequest parseSaveRequest(String line) {
        if (!line.startsWith(SAVE_PREFIX)) {
            throw new IllegalArgumentException("Invalid save request");
        }
        String payload = line.substring(SAVE_PREFIX.length());
        String[] parts = payload.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Malformed save request");
        }
        return new SaveRequest(decode(parts[0]), decode(parts[1]));
    }

    public static String buildLoadRequest(String saveName) {
        return LOAD_PREFIX + encode(saveName);
    }

    public static String parseLoadRequest(String line) {
        if (!line.startsWith(LOAD_PREFIX)) {
            throw new IllegalArgumentException("Invalid load request");
        }
        return decode(line.substring(LOAD_PREFIX.length()));
    }

    public static String buildInfo(String message) {
        return INFO_PREFIX + encode(message);
    }

    public static String parseInfo(String line) {
        if (!line.startsWith(INFO_PREFIX)) {
            throw new IllegalArgumentException("Invalid info message");
        }
        return decode(line.substring(INFO_PREFIX.length()));
    }

    public static String buildScreenshot(String fileName, byte[] pngBytes) {
        return SCREENSHOT_PREFIX + encode(fileName) + ":" + Base64.getEncoder().encodeToString(pngBytes);
    }

    public static ScreenshotPayload parseScreenshot(String line) {
        if (!line.startsWith(SCREENSHOT_PREFIX)) {
            throw new IllegalArgumentException("Invalid screenshot message");
        }
        String payload = line.substring(SCREENSHOT_PREFIX.length());
        String[] parts = payload.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Malformed screenshot message");
        }
        return new ScreenshotPayload(decode(parts[0]), Base64.getDecoder().decode(parts[1]));
    }

    public static String buildErrorJson(String message) {
        return "{\"error\":\"" + escapeJson(message) + "\"}";
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static final class SaveRequest {
        public final String dottedPath;
        public final String saveName;

        public SaveRequest(String dottedPath, String saveName) {
            this.dottedPath = dottedPath;
            this.saveName = saveName;
        }
    }

    public static final class ScreenshotPayload {
        public final String fileName;
        public final byte[] pngBytes;

        public ScreenshotPayload(String fileName, byte[] pngBytes) {
            this.fileName = fileName;
            this.pngBytes = pngBytes;
        }
    }
}
