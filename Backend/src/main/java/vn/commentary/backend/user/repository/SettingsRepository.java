package vn.commentary.backend.user.repository;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Đọc cấu hình dùng cho truy cập: {@code settings.accessHours} (bảng cũ do admin chỉnh) và
 * {@code app_config} (giới hạn thiết bị App).
 */
@Repository
public class SettingsRepository {
    private static final String MAX_APP_KEY = "max_app_devices";

    private final JdbcTemplate jdbc;

    public SettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int accessHours(int fallback) {
        try {
            List<Integer> rows = jdbc.queryForList("SELECT accessHours FROM settings LIMIT 1", Integer.class);
            if (!rows.isEmpty() && rows.get(0) != null && rows.get(0) > 0) {
                return rows.get(0);
            }
        } catch (DataAccessException ignored) {
            // bảng settings chưa có hoặc khác schema: dùng giá trị mặc định
        }
        return fallback;
    }

    public void setAccessHours(int hours) {
        int changed = jdbc.update("UPDATE settings SET accessHours = ?", hours);
        if (changed == 0) {
            jdbc.update("INSERT INTO settings (accessHours) VALUES (?)", hours);
        }
    }

    public int maxAppDevices(int fallback) {
        List<String> rows = jdbc.queryForList("SELECT config_value FROM app_config WHERE config_key = ?",
                String.class, MAX_APP_KEY);
        if (rows.isEmpty() || rows.get(0) == null) return fallback;
        try {
            return Integer.parseInt(rows.get(0).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void setMaxAppDevices(int value) {
        int changed = jdbc.update("UPDATE app_config SET config_value = ? WHERE config_key = ?",
                String.valueOf(value), MAX_APP_KEY);
        if (changed == 0) {
            jdbc.update("INSERT INTO app_config (config_key, config_value) VALUES (?, ?)", MAX_APP_KEY,
                    String.valueOf(value));
        }
    }
}
