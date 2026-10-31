package vn.commentary.backend.admin.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;

/**
 * Phát hành và kiểm tra token đăng nhập của quản trị viên (không trạng thái, ký HMAC-SHA256).
 * Dạng token: {@code base64url(email|hạnDùngGiây).base64url(chữKý)}. Không cần bảng DB; đổi khoá bí mật = thu hồi mọi token.
 */
public class AdminTokenService {
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    /** Thông tin đã xác thực trong một token còn hạn. */
    public record Claims(String email, long expiresAtEpochSec) {
    }

    private final byte[] secret;
    private final long ttlSeconds;
    private final Clock clock;

    public AdminTokenService(String secret, long ttlSeconds, Clock clock) {
        if (secret == null || secret.length() < 16) throw new IllegalArgumentException("Khoá ký token phải dài ít nhất 16 ký tự.");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
        this.clock = clock;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public String issue(String email) {
        long exp = clock.instant().getEpochSecond() + ttlSeconds;
        String payload = ENC.encodeToString((email + "|" + exp).getBytes(StandardCharsets.UTF_8));
        return payload + "." + ENC.encodeToString(sign(payload));
    }

    /** Trả về thông tin trong token nếu chữ ký đúng và chưa hết hạn; ngược lại rỗng. */
    public Optional<Claims> verify(String token) {
        if (token == null) return Optional.empty();
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) return Optional.empty();
        String payload = token.substring(0, dot);
        try {
            String sigPart = token.substring(dot + 1);
            byte[] given = DEC.decode(sigPart);
            // chỉ nhận dạng mã hoá chuẩn: ký tự cuối base64 có bit thừa nên nhiều chuỗi khác nhau giải ra cùng chữ ký
            if (!ENC.encodeToString(given).equals(sigPart)) return Optional.empty();
            if (!MessageDigest.isEqual(sign(payload), given)) return Optional.empty();
            String[] parts = new String(DEC.decode(payload), StandardCharsets.UTF_8).split("\\|");
            if (parts.length != 2) return Optional.empty();
            long exp = Long.parseLong(parts[1]);
            if (exp <= clock.instant().getEpochSecond()) return Optional.empty();
            return Optional.of(new Claims(parts[0], exp));
        } catch (IllegalArgumentException e) { // base64 hoặc số không hợp lệ
            return Optional.empty();
        }
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
