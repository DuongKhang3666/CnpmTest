package vn.commentary.backend.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.commentary.backend.admin.security.AdminAuthException;
import vn.commentary.backend.admin.security.AdminTokenService;
import vn.commentary.backend.admin.security.LoginThrottle;
import vn.commentary.backend.common.util.TokenHasher;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

/**
 * Đăng nhập quản trị. Tài khoản lấy từ cấu hình ({@code ADMIN_EMAIL}/{@code ADMIN_PASSWORD}), không còn cứng trong mã nguồn.
 * Nếu chưa đặt mật khẩu, khi khởi động sẽ sinh mật khẩu ngẫu nhiên và in ra log một lần.
 */
@Service
public class AdminAuthService {
    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    public record LoginResult(String token, String email, long expiresAtEpochSec) {
    }

    private final String email;
    private final String password;
    private final String apiKey;
    private final AdminTokenService tokens;
    private final LoginThrottle throttle;
    private final Clock clock;

    @Autowired
    public AdminAuthService(@Value("${admin.email:admin@gmail.com}") String email,
                            @Value("${admin.password:}") String password,
                            @Value("${admin.token-secret:}") String tokenSecret,
                            @Value("${admin.token-hours:12}") long tokenHours,
                            @Value("${admin.api-key:}") String apiKey,
                            @Value("${admin.login.max-failures:5}") int maxFailures,
                            @Value("${admin.login.window-seconds:300}") long windowSeconds) {
        this(email, password, tokenSecret, tokenHours, apiKey, maxFailures, windowSeconds, Clock.systemUTC());
    }

    public AdminAuthService(String email, String password, String tokenSecret, long tokenHours, String apiKey,
                            int maxFailures, long windowSeconds, Clock clock) {
        this.email = norm(email);
        String pw = password == null ? "" : password;
        if (pw.isEmpty()) {
            pw = TokenHasher.randomHex(8);
            log.warn("====================================================================");
            log.warn("ADMIN_PASSWORD chưa được cấu hình. Mật khẩu quản trị tạm thời cho lần chạy này:");
            log.warn("    tài khoản: {}   mật khẩu: {}", this.email, pw);
            log.warn("Hãy đặt ADMIN_EMAIL / ADMIN_PASSWORD (và ADMIN_TOKEN_SECRET) trong .env để cố định.");
            log.warn("====================================================================");
        }
        this.password = pw;
        String secret = tokenSecret == null ? "" : tokenSecret.trim();
        if (secret.length() < 16) {
            if (!secret.isEmpty()) log.warn("ADMIN_TOKEN_SECRET quá ngắn (<16 ký tự), dùng khoá ngẫu nhiên.");
            else log.warn("ADMIN_TOKEN_SECRET chưa đặt: phiên đăng nhập admin sẽ mất mỗi lần khởi động lại máy chủ.");
            secret = TokenHasher.randomHex(32);
        }
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.clock = clock;
        this.tokens = new AdminTokenService(secret, Math.max(1, tokenHours) * 3600, clock);
        this.throttle = new LoginThrottle(maxFailures, windowSeconds, clock);
    }

    /** Đăng nhập; {@code clientKey} (thường là IP) dùng để giới hạn số lần thử sai. */
    public LoginResult login(String inputEmail, String inputPassword, String clientKey) {
        long wait = throttle.retryAfterSeconds(clientKey);
        if (wait > 0) {
            throw new AdminAuthException(HttpStatus.TOO_MANY_REQUESTS, "too-many-attempts",
                    "Nhập sai quá nhiều lần. Thử lại sau " + wait + " giây.");
        }
        // Luôn so sánh cả hai để thời gian phản hồi không tiết lộ email có đúng hay không.
        boolean emailOk = TokenHasher.safeEquals(email, norm(inputEmail));
        boolean passOk = TokenHasher.safeEquals(password, inputPassword == null ? "" : inputPassword);
        if (!(emailOk & passOk)) {
            throttle.recordFailure(clientKey);
            throw new AdminAuthException(HttpStatus.UNAUTHORIZED, "bad-credentials", "Email hoặc mật khẩu không chính xác.");
        }
        throttle.reset(clientKey);
        String token = tokens.issue(email);
        return new LoginResult(token, email, clock.instant().getEpochSecond() + tokens.ttlSeconds());
    }

    /** Email của quản trị viên nếu token còn hợp lệ. */
    public Optional<String> authenticateToken(String token) {
        return tokens.verify(token).map(AdminTokenService.Claims::email);
    }

    /** Khoá máy-với-máy (header X-Admin-Key) cho script/CI; chỉ có hiệu lực khi đã đặt {@code admin.api-key}. */
    public boolean matchesApiKey(String key) {
        return !apiKey.isEmpty() && TokenHasher.safeEquals(apiKey, key);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
