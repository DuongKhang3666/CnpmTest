package vn.commentary.backend.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Bảng {@code translation_cache}: lưu bản dịch đã có để không gọi lại dịch vụ dịch. */
@Repository
public class TranslationCacheRepository {
    private final JdbcTemplate jdbc;

    public TranslationCacheRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> find(String cacheKey) {
        return jdbc.queryForList("SELECT translated_text FROM translation_cache WHERE cache_key = ?",
                String.class, cacheKey).stream().findFirst();
    }

    public void save(String cacheKey, String target, String translated, long now) {
        int changed = jdbc.update("UPDATE translation_cache SET translated_text = ?, created_at = ? WHERE cache_key = ?",
                translated, now, cacheKey);
        if (changed == 0) {
            jdbc.update("INSERT INTO translation_cache (cache_key, target, translated_text, created_at) VALUES (?,?,?,?)",
                    cacheKey, target, translated, now);
        }
    }
}
