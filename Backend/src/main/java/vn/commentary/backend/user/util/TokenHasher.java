package vn.commentary.backend.user.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Tạo token ngẫu nhiên và băm SHA-256; so sánh hằng thời gian để tránh timing attack. */
public final class TokenHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenHasher() {
    }

    public static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    /** Token phiên của thiết bị (64 ký tự hex). */
    public static String newSessionToken() {
        return randomHex(32);
    }

    /** Token trong mã QR cấp quyền (32 ký tự hex, giống bản Node cũ). */
    public static String newQrToken() {
        return randomHex(16);
    }

    public static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matchesHash(String raw, String expectedHash) {
        if (raw == null || expectedHash == null) return false;
        return MessageDigest.isEqual(
                sha256(raw).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean safeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
