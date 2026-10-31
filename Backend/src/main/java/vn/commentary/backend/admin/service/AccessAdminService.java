package vn.commentary.backend.admin.service;

import vn.commentary.backend.user.service.AccessTokenService;
import vn.commentary.backend.user.service.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.commentary.backend.common.exception.ResourceNotFoundException;
import vn.commentary.backend.user.dto.Dtos.AccessOverview;
import vn.commentary.backend.user.dto.Dtos.DeviceDto;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.repository.DeviceSessionRepository;
import vn.commentary.backend.user.repository.SettingsRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Nghiệp vụ quản trị truy cập thiết bị: QR, gia hạn, thu hồi, thống kê người đang dùng (UC6.3). */
@Service
public class AccessAdminService {
    private final DeviceSessionRepository sessions;
    private final AccessTokenService accessTokens;
    private final SettingsRepository settings;
    private final SessionService sessionService;
    private final Clock clock;
    private final int defaultAccessHours;
    private final int defaultMaxAppDevices;

    public AccessAdminService(DeviceSessionRepository sessions, AccessTokenService accessTokens,
                              SettingsRepository settings, SessionService sessionService, Clock clock,
                              @Value("${user.session.default-access-hours:24}") int defaultAccessHours,
                              @Value("${user.session.max-app-devices:5}") int defaultMaxAppDevices) {
        this.sessions = sessions;
        this.accessTokens = accessTokens;
        this.settings = settings;
        this.sessionService = sessionService;
        this.clock = clock;
        this.defaultAccessHours = defaultAccessHours;
        this.defaultMaxAppDevices = defaultMaxAppDevices;
    }

    public AccessOverview overview() {
        long now = clock.millis();
        long window = sessionService.onlineWindowMillis();
        List<DeviceDto> devices = sessions.findAll().stream().map(s -> toDto(s, now, window)).toList();
        int active = (int) devices.stream().filter(d -> d.online() && !d.expired()).count();
        int activeApps = (int) devices.stream()
                .filter(d -> d.online() && !d.expired() && "app".equals(d.clientType())).count();
        return new AccessOverview(accessTokens.current(), settings.accessHours(defaultAccessHours),
                settings.maxAppDevices(defaultMaxAppDevices), active, devices.size(), activeApps, devices);
    }

    public String currentQrToken() {
        return accessTokens.current();
    }

    public String rotateQrToken() {
        return accessTokens.rotate();
    }

    /** Gia hạn: cộng {@code hours} vào mốc lớn hơn giữa "bây giờ" và hạn hiện tại. */
    public DeviceDto extend(String deviceId, Integer hours) {
        int h = hours == null ? settings.accessHours(defaultAccessHours) : hours;
        if (h < 1 || h > 8760) throw new IllegalArgumentException("Số giờ gia hạn phải từ 1 đến 8760.");
        DeviceSession s = sessions.find(deviceId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết bị."));
        long now = clock.millis();
        long expiresAt = Math.max(now, s.expiresAt()) + h * 3_600_000L;
        sessions.setExpiry(deviceId, expiresAt, h);
        return toDto(sessions.find(deviceId).orElse(s), now, sessionService.onlineWindowMillis());
    }

    /** Thu hồi quyền của thiết bị và đổi QR để mã cũ không dùng lại được (giống hành vi cũ). */
    public String revoke(String deviceId) {
        if (sessions.delete(deviceId) == 0) throw new ResourceNotFoundException("Không tìm thấy thiết bị.");
        return accessTokens.rotate();
    }

    public int clearOffline() {
        long now = clock.millis();
        return sessions.deleteOfflineBefore(now - sessionService.onlineWindowMillis());
    }

    public void setMaxAppDevices(Integer max) {
        if (max == null || max < 1 || max > 1000) {
            throw new IllegalArgumentException("Giới hạn thiết bị App phải từ 1 đến 1000.");
        }
        settings.setMaxAppDevices(max);
    }

    /** Thời hạn mặc định cấp cho thiết bị mới (giờ); thiết bị đã có giữ nguyên hạn của mình. */
    public void setAccessHours(Integer hours) {
        if (hours == null || hours < 1 || hours > 8760) {
            throw new IllegalArgumentException("Thời hạn phải từ 1 đến 8760 giờ.");
        }
        settings.setAccessHours(hours);
    }

    private static DeviceDto toDto(DeviceSession s, long now, long window) {
        return new DeviceDto(s.deviceId(), s.name(), s.browser(), s.clientType(), s.ip(), s.platform(),
                s.language(), s.screen(), s.timezone(), Instant.ofEpochMilli(s.firstSeenAt()).toString(),
                Instant.ofEpochMilli(s.lastSeenAt()).toString(), Instant.ofEpochMilli(s.expiresAt()).toString(),
                s.accessHours(), s.isOnline(now, window), s.isExpired(now));
    }
}
