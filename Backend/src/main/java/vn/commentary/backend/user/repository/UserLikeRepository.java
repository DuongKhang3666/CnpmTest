package vn.commentary.backend.user.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.commentary.backend.user.dto.Dtos.LikeItem;

import java.time.Instant;
import java.util.List;

/** Bảng {@code user_likes}: yêu thích theo từng thiết bị (khoá chính device + loại + id). */
@Repository
public class UserLikeRepository {
    private final JdbcTemplate jdbc;

    public UserLikeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LikeItem> list(String deviceId) {
        return jdbc.query("SELECT target_type, target_id, name, created_at FROM user_likes "
                        + "WHERE device_id = ? ORDER BY created_at DESC",
                (rs, i) -> new LikeItem(rs.getString("target_type"), rs.getLong("target_id"), rs.getString("name"),
                        Instant.ofEpochMilli(rs.getLong("created_at")).toString()),
                deviceId);
    }

    /** Thêm yêu thích; gọi lại nhiều lần vẫn chỉ có một bản ghi. */
    public void add(String deviceId, String targetType, long targetId, String name, long createdAt) {
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_likes WHERE device_id = ? AND target_type = ? AND target_id = ?",
                Integer.class, deviceId, targetType, targetId);
        if (exists != null && exists > 0) return;
        try {
            jdbc.update("INSERT INTO user_likes (device_id, target_type, target_id, name, created_at) VALUES (?,?,?,?,?)",
                    deviceId, targetType, targetId, name, createdAt);
        } catch (DuplicateKeyException ignored) {
            // hai request đồng thời: bản ghi đã tồn tại, coi như thành công
        }
    }

    public int remove(String deviceId, String targetType, long targetId) {
        return jdbc.update("DELETE FROM user_likes WHERE device_id = ? AND target_type = ? AND target_id = ?",
                deviceId, targetType, targetId);
    }
}
