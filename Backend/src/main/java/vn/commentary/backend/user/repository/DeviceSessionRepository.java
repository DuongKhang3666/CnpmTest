package vn.commentary.backend.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import vn.commentary.backend.user.model.Models.DeviceSession;

import java.util.List;
import java.util.Optional;

/** Truy cập bảng {@code device_sessions}: chỉ làm việc với dữ liệu, không chứa luật nghiệp vụ. */
@Repository
public class DeviceSessionRepository {
    private static final String COLUMNS = "device_id, session_hash, name, browser, client_type, ip, platform, "
            + "language, screen, timezone, user_agent, first_seen_at, last_seen_at, expires_at, access_hours";

    private static final RowMapper<DeviceSession> MAPPER = (rs, i) -> new DeviceSession(
            rs.getString("device_id"), rs.getString("session_hash"), rs.getString("name"),
            rs.getString("browser"), rs.getString("client_type"), rs.getString("ip"),
            rs.getString("platform"), rs.getString("language"), rs.getString("screen"),
            rs.getString("timezone"), rs.getString("user_agent"), rs.getLong("first_seen_at"),
            rs.getLong("last_seen_at"), rs.getLong("expires_at"), rs.getInt("access_hours"));

    private final JdbcTemplate jdbc;

    public DeviceSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DeviceSession> find(String deviceId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM device_sessions WHERE device_id = ?", MAPPER, deviceId)
                .stream().findFirst();
    }

    public List<DeviceSession> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM device_sessions ORDER BY last_seen_at DESC", MAPPER);
    }

    /** Thêm mới hoặc cập nhật theo device_id. */
    public void save(DeviceSession s) {
        int changed = jdbc.update("UPDATE device_sessions SET session_hash = ?, name = ?, browser = ?, "
                        + "client_type = ?, ip = ?, platform = ?, language = ?, screen = ?, timezone = ?, "
                        + "user_agent = ?, last_seen_at = ?, expires_at = ?, access_hours = ? WHERE device_id = ?",
                s.sessionHash(), s.name(), s.browser(), s.clientType(), s.ip(), s.platform(), s.language(),
                s.screen(), s.timezone(), s.userAgent(), s.lastSeenAt(), s.expiresAt(), s.accessHours(),
                s.deviceId());
        if (changed == 0) {
            jdbc.update("INSERT INTO device_sessions (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    s.deviceId(), s.sessionHash(), s.name(), s.browser(), s.clientType(), s.ip(), s.platform(),
                    s.language(), s.screen(), s.timezone(), s.userAgent(), s.firstSeenAt(), s.lastSeenAt(),
                    s.expiresAt(), s.accessHours());
        }
    }

    public void setLastSeen(String deviceId, long lastSeenAt) {
        jdbc.update("UPDATE device_sessions SET last_seen_at = ? WHERE device_id = ?", lastSeenAt, deviceId);
    }

    public int setExpiry(String deviceId, long expiresAt, int accessHours) {
        return jdbc.update("UPDATE device_sessions SET expires_at = ?, access_hours = ? WHERE device_id = ?",
                expiresAt, accessHours, deviceId);
    }

    public int delete(String deviceId) {
        return jdbc.update("DELETE FROM device_sessions WHERE device_id = ?", deviceId);
    }

    /** Xoá các thiết bị không còn online (last_seen trước mốc {@code onlineSince}). */
    public int deleteOfflineBefore(long onlineSince) {
        return jdbc.update("DELETE FROM device_sessions WHERE last_seen_at < ?", onlineSince);
    }

    /** Số thiết bị App còn quyền và đang online, không tính {@code excludeDeviceId}. */
    public int countOnlineApps(long now, long onlineSince, String excludeDeviceId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM device_sessions WHERE client_type = 'app' "
                + "AND expires_at > ? AND last_seen_at >= ? AND device_id <> ?", Integer.class,
                now, onlineSince, excludeDeviceId == null ? "" : excludeDeviceId);
        return n == null ? 0 : n;
    }
}
