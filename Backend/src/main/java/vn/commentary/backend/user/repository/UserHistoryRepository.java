package vn.commentary.backend.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.commentary.backend.user.dto.Dtos.HistoryItem;

import java.time.Instant;
import java.util.List;

/** Bảng {@code user_history}: lịch sử nghe theo từng thiết bị. */
@Repository
public class UserHistoryRepository {
    private final JdbcTemplate jdbc;

    public UserHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(String deviceId, String targetType, long targetId, String name, String language,
                    String method, long playedAt) {
        jdbc.update("INSERT INTO user_history (device_id, target_type, target_id, name, language, method, played_at) "
                + "VALUES (?,?,?,?,?,?,?)", deviceId, targetType, targetId, name, language, method, playedAt);
    }

    public List<HistoryItem> list(String deviceId, int limit) {
        return jdbc.query("SELECT id, target_type, target_id, name, language, method, played_at "
                        + "FROM user_history WHERE device_id = ? ORDER BY played_at DESC, id DESC LIMIT ?",
                (rs, i) -> new HistoryItem(rs.getLong("id"), rs.getString("target_type"), rs.getLong("target_id"),
                        rs.getString("name"), rs.getString("language"), rs.getString("method"),
                        Instant.ofEpochMilli(rs.getLong("played_at")).toString()),
                deviceId, limit);
    }

    public int clear(String deviceId) {
        return jdbc.update("DELETE FROM user_history WHERE device_id = ?", deviceId);
    }
}
