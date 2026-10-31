package vn.commentary.backend.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Bảng {@code access_tokens}: token nằm trong mã QR cấp quyền cho thiết bị mới. */
@Repository
public class AccessTokenRepository {
    private final JdbcTemplate jdbc;

    public AccessTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> findActive() {
        return jdbc.queryForList("SELECT token FROM access_tokens WHERE active = TRUE ORDER BY id DESC LIMIT 1",
                String.class).stream().findFirst();
    }

    public void deactivateAll() {
        jdbc.update("UPDATE access_tokens SET active = FALSE WHERE active = TRUE");
    }

    public void insert(String token, long createdAt) {
        jdbc.update("INSERT INTO access_tokens (token, active, created_at) VALUES (?, TRUE, ?)", token, createdAt);
    }
}
