package vn.commentary.backend.user.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Các mô hình miền (entity) của Back-end user. Gom trong một lớp để gọn; mỗi record là một entity. */
public final class Models {
    private Models() {
    }

    /** Loại đối tượng thuyết minh. */
    public enum TargetType {
        SITE("site", "sites"),
        ARTIFACT("artifact", "artifacts");

        private final String code;
        private final String table;

        TargetType(String code, String table) {
            this.code = code;
            this.table = table;
        }

        public String code() {
            return code;
        }

        public String table() {
            return table;
        }

        public static TargetType from(String value) {
            if (value != null) {
                String v = value.trim().toLowerCase(Locale.ROOT);
                for (TargetType t : values()) {
                    if (t.code.equals(v)) return t;
                }
            }
            throw new IllegalArgumentException("targetType phải là 'site' hoặc 'artifact'.");
        }
    }

    /** Phiên truy cập của một thiết bị (đã quét QR hợp lệ). Thời gian tính bằng epoch millis. */
    public record DeviceSession(String deviceId, String sessionHash, String name, String browser,
                                String clientType, String ip, String platform, String language,
                                String screen, String timezone, String userAgent,
                                long firstSeenAt, long lastSeenAt, long expiresAt, int accessHours) {
        public boolean isExpired(long now) {
            return now >= expiresAt;
        }

        public boolean isOnline(long now, long windowMillis) {
            return lastSeenAt >= now - windowMillis && !isExpired(now);
        }

        public boolean isApp() {
            return "app".equals(clientType);
        }
    }

    public record Language(long id, String name, String code, String ttsKey, boolean enabled) {
    }

    public record Site(long id, String name, String classify, String city, String address,
                       String description, Double latitude, Double longitude, boolean hasImage,
                       List<String> extraImages, boolean hidden, String updatedAt) {
    }

    public record Artifact(long id, long siteId, String name, String address, String description,
                           Double latitude, Double longitude, boolean hasImage, boolean hidden,
                           String updatedAt) {
    }

    public record GeoConfig(boolean enabled, double latitude, double longitude, double radius) {
        public static GeoConfig disabled() {
            return new GeoConfig(false, 0, 0, 0);
        }
    }

    public record BeaconConfig(boolean enabled, String deviceName, String mac, double radius) {
        public static BeaconConfig disabled() {
            return new BeaconConfig(false, "", "", 0);
        }
    }

    public record QrConfig(boolean enabled, String code, String link) {
        public static QrConfig disabled() {
            return new QrConfig(false, "", "");
        }
    }

    public record Narration(long id, TargetType targetType, long targetId, String text,
                            boolean useDescription, boolean ttsEnabled, boolean audioEnabled,
                            String audioName, boolean hasAudio, String language, double speed,
                            GeoConfig gps, BeaconConfig bluetooth, QrConfig qr,
                            Map<String, String> translations) {
    }

    /** Dữ liệu audio thô của một thuyết minh. */
    public record Audio(String name, String data) {
    }

    /** Đối tượng đang hiển thị công khai (đã xuất bản, không bị ẩn) dùng chung cho tra cứu. */
    public record TargetInfo(TargetType type, long id, String name, String description,
                             Long siteId, String siteName) {
    }
}
