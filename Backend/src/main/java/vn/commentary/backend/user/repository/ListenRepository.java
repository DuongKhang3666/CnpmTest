package vn.commentary.backend.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Bảng {@code listens} (dùng chung với báo cáo của admin): mỗi dòng = lượt nghe của 1 đối tượng/ngày/ngôn ngữ. */
@Repository
public class ListenRepository {
    private final JdbcTemplate jdbc;

    public ListenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Tăng lượt nghe thêm 1; tạo dòng mới nếu chưa có. Đồng bộ hoá để không trùng id trong một tiến trình. */
    public synchronized void increment(String date, String targetType, long targetId, String language) {
        int changed = jdbc.update("UPDATE listens SET count = count + 1 "
                + "WHERE date = ? AND targetType = ? AND targetId = ? AND language = ?",
                date, targetType, targetId, language);
        if (changed == 0) {
            Long next = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM listens", Long.class);
            jdbc.update("INSERT INTO listens (id, date, targetType, targetId, language, count) VALUES (?,?,?,?,?,1)",
                    next, date, targetType, targetId, language);
        }
    }

    public long total(String targetType, long targetId) {
        Long n = jdbc.queryForObject("SELECT COALESCE(SUM(count), 0) FROM listens WHERE targetType = ? AND targetId = ?",
                Long.class, targetType, targetId);
        return n == null ? 0 : n;
    }

    /** Tổng lượt nghe theo từng đối tượng (gộp mọi ngày và ngôn ngữ). */
    public java.util.List<TargetTotal> totalsByTarget() {
        return jdbc.query("SELECT targetType, targetId, SUM(count) AS total FROM listens GROUP BY targetType, targetId",
                (rs, i) -> new TargetTotal(rs.getString("targetType"), rs.getLong("targetId"), rs.getLong("total")));
    }

    public record TargetTotal(String targetType, long targetId, long count) {
    }
}
