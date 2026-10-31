package vn.commentary.backend.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.commentary.backend.user.dto.Dtos.DeviceInfo;
import vn.commentary.backend.user.dto.Dtos.JoinRequest;
import vn.commentary.backend.user.dto.Dtos.JoinResponse;
import vn.commentary.backend.user.dto.Dtos.SessionStatus;
import vn.commentary.backend.user.exception.UserAccessException;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.repository.DeviceSessionRepository;
import vn.commentary.backend.user.repository.SettingsRepository;
import vn.commentary.backend.user.util.DeviceDescriber;
import vn.commentary.backend.user.util.NetworkPolicy;
import vn.commentary.backend.user.util.TokenHasher;

import java.time.Clock;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Quy tắc truy cập của thiết bị người dùng (thay cho phần "user" của server.mjs):
 * quét QR hợp lệ → được cấp token phiên → dùng token ở mọi lời gọi tiếp theo cho tới khi hết hạn.
 */
@Service
public class SessionService {
    private static final Pattern DEVICE_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final long TOUCH_INTERVAL_MS = 5_000;

    private final DeviceSessionRepository sessions;
    private final AccessTokenService accessTokens;
    private final SettingsRepository settings;
    private final Clock clock;
    private final long onlineWindowMs;
    private final int defaultAccessHours;
    private final int defaultMaxAppDevices;
    private final boolean lanOnly;

    public SessionService(DeviceSessionRepository sessions, AccessTokenService accessTokens,
                          SettingsRepository settings, Clock clock,
                          @Value("${user.session.online-window-seconds:45}") long onlineWindowSeconds,
                          @Value("${user.session.default-access-hours:24}") int defaultAccessHours,
                          @Value("${user.session.max-app-devices:5}") int defaultMaxAppDevices,
                          @Value("${user.lan-only:true}") boolean lanOnly) {
        this.sessions = sessions;
        this.accessTokens = accessTokens;
        this.settings = settings;
        this.clock = clock;
        this.onlineWindowMs = Math.max(5, onlineWindowSeconds) * 1000L;
        this.defaultAccessHours = defaultAccessHours;
        this.defaultMaxAppDevices = defaultMaxAppDevices;
        this.lanOnly = lanOnly;
    }

    public long onlineWindowMillis() {
        return onlineWindowMs;
    }

    /** Thiết bị vào hệ thống bằng token QR (lần đầu) hoặc bằng token phiên cũ (vào lại). */
    public JoinResponse join(JoinRequest request, String ip) {
        if (lanOnly && !NetworkPolicy.isPrivateOrLoopback(ip)) {
            throw new UserAccessException(HttpStatus.FORBIDDEN, "not-lan",
                    "Hãy kết nối cùng mạng Wi-Fi/LAN với máy chủ.");
        }
        String deviceId = request == null ? null : request.deviceId();
        if (deviceId == null || !DEVICE_ID.matcher(deviceId).matches()) {
            throw new UserAccessException(HttpStatus.BAD_REQUEST, "bad-device", "Mã thiết bị không hợp lệ.");
        }
        long now = clock.millis();
        DeviceSession old = sessions.find(deviceId).orElse(null);
        if (old != null && old.isExpired(now)) {
            throw new UserAccessException(HttpStatus.FORBIDDEN, "expired",
                    "Quyền truy cập của thiết bị đã hết hạn. Hãy quét mã QR mới hoặc nhờ quản trị viên gia hạn.");
        }

        boolean sessionOk = old != null && TokenHasher.matchesHash(request.sessionToken(), old.sessionHash());
        boolean qrOk = accessTokens.matches(request.token());
        if (!sessionOk && !qrOk) {
            boolean noToken = request.token() == null || request.token().isBlank();
            throw new UserAccessException(HttpStatus.UNAUTHORIZED, noToken ? "no-token" : "token-invalid",
                    noToken ? "Vui lòng quét mã QR do quản trị viên cung cấp."
                            : "Mã QR không hợp lệ hoặc đã bị thay đổi.");
        }

        DeviceInfo info = request.info() == null ? new DeviceInfo(null, null, null, null, null, null, null)
                : request.info();
        String clientType = "app".equals(info.clientType()) ? "app" : "web";

        if ("app".equals(clientType) && (old == null || !old.isOnline(now, onlineWindowMs))) {
            int max = settings.maxAppDevices(defaultMaxAppDevices);
            int online = sessions.countOnlineApps(now, now - onlineWindowMs, deviceId);
            if (online >= max) {
                throw new UserAccessException(HttpStatus.FORBIDDEN, "app-limit",
                        "Đã đạt giới hạn " + max + " thiết bị App đang đăng nhập.");
            }
        }

        int hours = old != null ? old.accessHours() : settings.accessHours(defaultAccessHours);
        long expiresAt = old != null ? old.expiresAt() : now + hours * 3_600_000L;
        String ua = cut(info.userAgent(), 400);
        DeviceDescriber.Description described = DeviceDescriber.describe(ua);
        String name = cut(info.deviceName(), 100);
        // Vào lại bằng phiên còn hợp lệ: giữ nguyên token (không xoay) để nhiều tab/lời gọi đồng thời
        // không ghi đè nhau làm token trong máy khách lệch với token ở máy chủ.
        String rawToken = sessionOk ? request.sessionToken() : TokenHasher.newSessionToken();

        DeviceSession session = new DeviceSession(deviceId, TokenHasher.sha256(rawToken),
                name.isEmpty() ? described.name() : name,
                "app".equals(clientType) ? "Ứng dụng React Native" : described.browser(),
                clientType, cut(ip, 64), cut(info.platform(), 60), cut(info.language(), 20),
                cut(info.screen(), 20), cut(info.timezone(), 60), ua,
                old != null ? old.firstSeenAt() : now, now, expiresAt, hours);
        sessions.save(session);
        return new JoinResponse(true, deviceId, rawToken, Instant.ofEpochMilli(expiresAt).toString(), hours);
    }

    /** Xác thực mỗi request: thiết bị phải có phiên đúng token và chưa hết hạn. */
    public DeviceSession authenticate(String deviceId, String sessionToken) {
        if (deviceId == null || deviceId.isBlank() || sessionToken == null || sessionToken.isBlank()) {
            throw new UserAccessException(HttpStatus.UNAUTHORIZED, "no-session",
                    "Thiếu thông tin phiên. Hãy quét mã QR để vào hệ thống.");
        }
        DeviceSession session = sessions.find(deviceId).orElseThrow(() ->
                new UserAccessException(HttpStatus.UNAUTHORIZED, "no-session",
                        "Thiết bị chưa được cấp quyền hoặc đã bị thu hồi. Hãy quét mã QR lại."));
        if (!TokenHasher.matchesHash(sessionToken, session.sessionHash())) {
            throw new UserAccessException(HttpStatus.UNAUTHORIZED, "invalid-session",
                    "Phiên không hợp lệ. Hãy quét mã QR lại.");
        }
        long now = clock.millis();
        if (session.isExpired(now)) {
            throw new UserAccessException(HttpStatus.FORBIDDEN, "expired",
                    "Quyền truy cập đã hết hạn. Hãy quét mã QR mới hoặc nhờ quản trị viên gia hạn.");
        }
        if (now - session.lastSeenAt() >= TOUCH_INTERVAL_MS) {
            sessions.setLastSeen(deviceId, now);
        }
        return session;
    }

    public SessionStatus status(DeviceSession session) {
        long now = clock.millis();
        long remaining = Math.max(0, (session.expiresAt() - now) / 1000);
        return new SessionStatus(true, session.deviceId(), session.clientType(),
                Instant.ofEpochMilli(session.expiresAt()).toString(), remaining);
    }

    /** Thiết bị rời hệ thống: đánh dấu offline ngay (không thu hồi quyền). */
    public void leave(DeviceSession session) {
        sessions.setLastSeen(session.deviceId(), clock.millis() - onlineWindowMs - 1);
    }

    private static String cut(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }
}
