package vn.commentary.backend.user.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Giải mã chuỗi {@code data:<mime>;base64,<payload>} (ảnh/audio lưu trực tiếp trong DB). */
public final class DataUrl {
    public record Parsed(String mime, byte[] bytes) {
    }

    private DataUrl() {
    }

    public static boolean isDataUrl(String value) {
        return value != null && value.startsWith("data:");
    }

    public static Parsed parse(String value) {
        if (!isDataUrl(value)) throw new IllegalArgumentException("Không phải data URL.");
        int comma = value.indexOf(',');
        if (comma < 0) throw new IllegalArgumentException("Data URL thiếu dấu phẩy.");
        String meta = value.substring(5, comma);
        String payload = value.substring(comma + 1);
        String mime = meta.split(";", -1)[0].trim();
        if (mime.isEmpty()) mime = "application/octet-stream";
        boolean base64 = meta.toLowerCase().contains(";base64");
        byte[] bytes = base64
                ? Base64.getMimeDecoder().decode(payload)
                : URLDecoder.decode(payload, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        return new Parsed(mime, bytes);
    }
}
