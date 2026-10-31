package vn.commentary.backend.user.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import vn.commentary.backend.user.model.Models.Artifact;
import vn.commentary.backend.user.model.Models.Audio;
import vn.commentary.backend.user.model.Models.BeaconConfig;
import vn.commentary.backend.user.model.Models.GeoConfig;
import vn.commentary.backend.user.model.Models.Language;
import vn.commentary.backend.user.model.Models.Narration;
import vn.commentary.backend.user.model.Models.QrConfig;
import vn.commentary.backend.user.model.Models.Site;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.util.RowMaps;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static vn.commentary.backend.user.util.RowMaps.bool;
import static vn.commentary.backend.user.util.RowMaps.dbl;
import static vn.commentary.backend.user.util.RowMaps.lng;
import static vn.commentary.backend.user.util.RowMaps.str;

/**
 * Đọc dữ liệu nội dung (chỉ đọc) cho người dùng. Các cột ảnh/audio rất lớn (data URL base64) KHÔNG được
 * kéo vào danh sách: chỉ lấy độ dài để biết "có ảnh hay không", ảnh thật lấy riêng qua {@link #findImage}.
 */
@Repository
public class ContentRepository {
    private static final String SITE_COLUMNS = "id, name, classify, city, address, description, latitude, "
            + "longitude, status, hidden, updatedAt, COALESCE(CHAR_LENGTH(img), 0) AS img_len";
    private static final String ARTIFACT_COLUMNS = "id, siteId, name, address, description, latitude, "
            + "longitude, status, hidden, updatedAt, COALESCE(CHAR_LENGTH(img), 0) AS img_len";

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final Map<String, Boolean> columnCache = new ConcurrentHashMap<>();

    public ContentRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    /* ---------- Ngôn ngữ ---------- */
    public List<Language> findLanguages() {
        return jdbc.query("SELECT id, name, code, ttsKey, enabled FROM languages ORDER BY id", (rs, i) -> {
            Map<String, Object> r = RowMaps.toMap(rs);
            return new Language(lng(r, "id"), str(r, "name"), str(r, "code"), str(r, "ttsKey"), bool(r, "enabled"));
        });
    }

    /* ---------- Địa điểm ---------- */
    public List<Site> findPublishedSites() {
        return jdbc.query("SELECT " + SITE_COLUMNS + " FROM sites WHERE status = 'published' ORDER BY id",
                (rs, i) -> toSite(RowMaps.toMap(rs)));
    }

    public Optional<Site> findPublishedSite(long id, boolean withExtraImages) {
        String cols = SITE_COLUMNS + (withExtraImages ? ", extraImages" : "");
        return jdbc.query("SELECT " + cols + " FROM sites WHERE id = ? AND status = 'published'",
                (rs, i) -> toSite(RowMaps.toMap(rs)), id).stream().findFirst();
    }

    /* ---------- Hiện vật ---------- */
    public List<Artifact> findPublishedArtifacts() {
        return jdbc.query("SELECT " + ARTIFACT_COLUMNS + " FROM artifacts WHERE status = 'published' ORDER BY id",
                (rs, i) -> toArtifact(RowMaps.toMap(rs)));
    }

    public Optional<Artifact> findPublishedArtifact(long id) {
        return jdbc.query("SELECT " + ARTIFACT_COLUMNS + " FROM artifacts WHERE id = ? AND status = 'published'",
                (rs, i) -> toArtifact(RowMaps.toMap(rs)), id).stream().findFirst();
    }

    /* ---------- Ảnh ---------- */
    public Optional<String> findImage(TargetType type, long id) {
        List<String> rows = jdbc.queryForList("SELECT img FROM " + type.table() + " WHERE id = ?", String.class, id);
        return rows.stream().filter(v -> v != null && !v.isBlank()).findFirst();
    }

    public List<String> findExtraImages(long siteId) {
        List<String> rows = jdbc.queryForList("SELECT extraImages FROM sites WHERE id = ?", String.class, siteId);
        if (rows.isEmpty()) return List.of();
        return parseStringList(rows.get(0));
    }

    /* ---------- Thuyết minh ---------- */
    public List<Narration> findAllNarrations() {
        return jdbc.query("SELECT " + narrationColumns() + " FROM narrations ORDER BY id",
                        (rs, i) -> toNarrationOrNull(RowMaps.toMap(rs)))
                .stream().filter(java.util.Objects::nonNull).toList();
    }

    public Optional<Narration> findNarration(TargetType type, long targetId) {
        return jdbc.query("SELECT " + narrationColumns() + " FROM narrations WHERE targetType = ? AND targetId = ? "
                        + "ORDER BY id LIMIT 1",
                (rs, i) -> toNarration(RowMaps.toMap(rs)), type.code(), targetId).stream().findFirst();
    }

    public Optional<Audio> findAudio(TargetType type, long targetId) {
        return jdbc.query("SELECT audioName, audioData FROM narrations WHERE targetType = ? AND targetId = ? "
                        + "ORDER BY id LIMIT 1",
                (rs, i) -> new Audio(rs.getString("audioName"), rs.getString("audioData")),
                type.code(), targetId).stream().findFirst();
    }

    /* ---------- Chuyển dòng DB thành entity ---------- */
    private Site toSite(Map<String, Object> r) {
        List<String> extra = r.containsKey("extraimages") ? parseStringList(str(r, "extraImages")) : List.of();
        return new Site(lng(r, "id"), str(r, "name"), str(r, "classify"), str(r, "city"), str(r, "address"),
                str(r, "description"), dbl(r, "latitude"), dbl(r, "longitude"), lng(r, "img_len") > 0,
                extra, bool(r, "hidden"), str(r, "updatedAt"));
    }

    private Artifact toArtifact(Map<String, Object> r) {
        return new Artifact(lng(r, "id"), lng(r, "siteId"), str(r, "name"), str(r, "address"),
                str(r, "description"), dbl(r, "latitude"), dbl(r, "longitude"), lng(r, "img_len") > 0,
                bool(r, "hidden"), str(r, "updatedAt"));
    }

    /** Bỏ qua dòng thuyết minh có targetType lạ thay vì làm hỏng cả danh sách. */
    private Narration toNarrationOrNull(Map<String, Object> r) {
        try {
            return toNarration(r);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Narration toNarration(Map<String, Object> r) {
        String lang = str(r, "language").trim();
        Double speed = dbl(r, "speed");
        double clamped = speed == null || speed <= 0 ? 1.0 : Math.min(4.0, Math.max(0.1, speed));
        return new Narration(lng(r, "id"), TargetType.from(str(r, "targetType")), lng(r, "targetId"),
                str(r, "text"), bool(r, "useDescription"), bool(r, "ttsEnabled"), bool(r, "audioEnabled"),
                str(r, "audioName"), lng(r, "audio_len") > 0, lang.isEmpty() ? "vi" : lang, clamped,
                geo(json(str(r, "gps"))), beacon(json(str(r, "bluetooth"))), qr(json(str(r, "qr"))),
                translations(json(str(r, "translations"))));
    }

    private String narrationColumns() {
        StringBuilder sb = new StringBuilder("id, targetType, targetId, text, useDescription, ttsEnabled, "
                + "audioEnabled, audioName, COALESCE(CHAR_LENGTH(audioData), 0) AS audio_len, language, speed, "
                + "gps, bluetooth, qr");
        if (hasColumn("narrations", "translations")) sb.append(", translations");
        return sb.toString();
    }

    /** Kiểm tra cột có tồn tại không (bảng cũ có thể chưa có cột translations). Kết quả được nhớ lại. */
    private boolean hasColumn(String table, String column) {
        return columnCache.computeIfAbsent(table + "." + column, k -> {
            ResultSetExtractor<Boolean> extractor = rs -> {
                ResultSetMetaData md = rs.getMetaData();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    if (md.getColumnLabel(i).equalsIgnoreCase(column)) return true;
                }
                return false;
            };
            Boolean found = jdbc.query("SELECT * FROM " + table + " WHERE 1 = 0", extractor);
            return Boolean.TRUE.equals(found);
        });
    }

    /* ---------- JSON mềm dẻo: dữ liệu cũ có thể là text, JSON, hoặc JSON bị mã hoá hai lần ---------- */
    private JsonNode json(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            JsonNode node = mapper.readTree(raw);
            if (node != null && node.isTextual()) node = mapper.readTree(node.asText());
            return node == null || node.isMissingNode() || node.isNull() ? null : node;
        } catch (Exception e) {
            return null;
        }
    }

    private GeoConfig geo(JsonNode n) {
        if (n == null || !n.isObject() || !n.hasNonNull("latitude") || !n.hasNonNull("longitude")) {
            return GeoConfig.disabled();
        }
        return new GeoConfig(n.path("enabled").asBoolean(false), n.path("latitude").asDouble(),
                n.path("longitude").asDouble(), n.path("radius").asDouble(0));
    }

    private BeaconConfig beacon(JsonNode n) {
        if (n == null || !n.isObject()) return BeaconConfig.disabled();
        return new BeaconConfig(n.path("enabled").asBoolean(false), n.path("deviceName").asText(""),
                n.path("mac").asText(""), n.path("radius").asDouble(0));
    }

    private QrConfig qr(JsonNode n) {
        if (n == null || !n.isObject()) return QrConfig.disabled();
        return new QrConfig(n.path("enabled").asBoolean(false), n.path("code").asText(""), n.path("link").asText(""));
    }

    private Map<String, String> translations(JsonNode n) {
        Map<String, String> out = new LinkedHashMap<>();
        if (n != null && n.isObject()) {
            n.fields().forEachRemaining(e -> {
                if (e.getValue().isTextual() && !e.getValue().asText().isBlank()) {
                    out.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().asText());
                }
            });
        }
        return out;
    }

    private List<String> parseStringList(String raw) {
        JsonNode n = json(raw);
        List<String> out = new ArrayList<>();
        if (n != null && n.isArray()) {
            for (JsonNode item : n) {
                if (item.isTextual() && !item.asText().isBlank()) out.add(item.asText());
            }
        }
        return out;
    }
}
