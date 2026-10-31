package vn.commentary.backend.admin.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Repairs only the placeholder-only likes table produced when the original JSON array was empty. */
@Component
public class DatabaseCompatibilityInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public DatabaseCompatibilityInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> tables = jdbc.queryForList(
                "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'likes'");
        if (tables.isEmpty()) {
            jdbc.execute("CREATE TABLE likes (id BIGINT NOT NULL PRIMARY KEY, targetType LONGTEXT NOT NULL, targetId BIGINT NOT NULL, name LONGTEXT NOT NULL, createdAt LONGTEXT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        }
        List<Map<String, Object>> cols = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'likes' ORDER BY ORDINAL_POSITION");
        if (cols.size() == 1 && "_empty_placeholder".equalsIgnoreCase(String.valueOf(cols.get(0).get("COLUMN_NAME")))) {
            jdbc.execute("ALTER TABLE likes DROP COLUMN _empty_placeholder");
            jdbc.execute("ALTER TABLE likes ADD COLUMN id BIGINT NOT NULL PRIMARY KEY, ADD COLUMN targetType LONGTEXT NOT NULL, ADD COLUMN targetId BIGINT NOT NULL, ADD COLUMN name LONGTEXT NOT NULL, ADD COLUMN createdAt LONGTEXT NOT NULL");
        }
        Long settingsCount = jdbc.queryForObject("SELECT COUNT(*) FROM settings", Long.class);
        if (settingsCount != null && settingsCount == 0L) jdbc.update("INSERT INTO settings (accessHours) VALUES (24)");
    }
}
