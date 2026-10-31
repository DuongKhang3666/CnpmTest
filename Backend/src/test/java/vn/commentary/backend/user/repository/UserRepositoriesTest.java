package vn.commentary.backend.user.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import vn.commentary.backend.user.dto.Dtos.HistoryItem;
import vn.commentary.backend.user.dto.Dtos.LikeItem;
import vn.commentary.backend.user.model.Models.Artifact;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.model.Models.Narration;
import vn.commentary.backend.user.model.Models.Site;
import vn.commentary.backend.user.model.Models.TargetType;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm thử tầng repository với H2 (chế độ MySQL) dùng đúng các file schema của ứng dụng. */
class UserRepositoriesTest {
    private static JdbcTemplate jdbc;
    private static ContentRepository content;

    @BeforeAll
    static void initDatabase() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:usertest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/base-schema.sql"), new ClassPathResource("db/user-schema.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(ds);
        jdbc = new JdbcTemplate(ds);
        content = new ContentRepository(jdbc, new ObjectMapper());
    }

    @BeforeEach
    void clean() {
        for (String t : List.of("sites", "artifacts", "narrations", "languages", "listens", "settings", "access_tokens",
                "device_sessions", "app_config", "translation_cache", "user_history", "user_likes")) {
            jdbc.update("DELETE FROM " + t);
        }
    }

    /* ---------- phiên thiết bị ---------- */
    private static DeviceSession session(String id, String type, long lastSeen, long expires) {
        return new DeviceSession(id, "hash-" + id, "Tên", "Chrome", type, "1.2.3.4", "p", "vi", "1x1", "tz", "ua",
                1000, lastSeen, expires, 24);
    }

    @Test
    void deviceSessionSaveFindUpdateDelete() {
        DeviceSessionRepository repo = new DeviceSessionRepository(jdbc);
        assertTrue(repo.find("dev-00000001").isEmpty());

        repo.save(session("dev-00000001", "web", 5000, 9000));
        DeviceSession loaded = repo.find("dev-00000001").orElseThrow();
        assertEquals("hash-dev-00000001", loaded.sessionHash());
        assertEquals(9000, loaded.expiresAt());

        repo.save(new DeviceSession("dev-00000001", "hash-moi", "Mới", "Edge", "web", "9.9.9.9", "", "", "", "", "",
                1000, 6000, 12000, 48));
        DeviceSession updated = repo.find("dev-00000001").orElseThrow();
        assertEquals("hash-moi", updated.sessionHash());
        assertEquals(48, updated.accessHours());
        assertEquals(1, repo.findAll().size());

        repo.setLastSeen("dev-00000001", 7000);
        assertEquals(7000, repo.find("dev-00000001").orElseThrow().lastSeenAt());
        assertEquals(1, repo.setExpiry("dev-00000001", 20000, 5));
        assertEquals(1, repo.delete("dev-00000001"));
        assertEquals(0, repo.delete("dev-00000001"));
    }

    @Test
    void countOnlineAppsOnlyCountsLiveUnexpiredAppsExcludingSelf() {
        DeviceSessionRepository repo = new DeviceSessionRepository(jdbc);
        long now = 100_000;
        repo.save(session("app-00000001", "app", 99_000, 500_000)); // online
        repo.save(session("app-00000002", "app", 10_000, 500_000)); // offline
        repo.save(session("app-00000003", "app", 99_000, 50_000)); // hết hạn
        repo.save(session("web-00000001", "web", 99_000, 500_000)); // không phải app
        assertEquals(1, repo.countOnlineApps(now, now - 45_000, "khac"));
        assertEquals(0, repo.countOnlineApps(now, now - 45_000, "app-00000001"));
        assertEquals(1, repo.deleteOfflineBefore(now - 45_000)); // chỉ app-00000002 có last_seen trước mốc online
    }

    @Test
    void accessTokenLifecycle() {
        AccessTokenRepository repo = new AccessTokenRepository(jdbc);
        assertTrue(repo.findActive().isEmpty());
        repo.insert("tok-1", 1);
        assertEquals("tok-1", repo.findActive().orElseThrow());
        repo.deactivateAll();
        assertTrue(repo.findActive().isEmpty());
        repo.insert("tok-2", 2);
        assertEquals("tok-2", repo.findActive().orElseThrow());
    }

    @Test
    void settingsFallbackAndOverride() {
        SettingsRepository repo = new SettingsRepository(jdbc);
        assertEquals(24, repo.accessHours(24));
        jdbc.update("INSERT INTO settings (accessHours) VALUES (72)");
        assertEquals(72, repo.accessHours(24));

        assertEquals(5, repo.maxAppDevices(5));
        repo.setMaxAppDevices(8);
        repo.setMaxAppDevices(9);
        assertEquals(9, repo.maxAppDevices(5));
    }

    @Test
    void translationCacheUpsert() {
        TranslationCacheRepository repo = new TranslationCacheRepository(jdbc);
        assertTrue(repo.find("k").isEmpty());
        repo.save("k", "en", "một", 1);
        repo.save("k", "en", "hai", 2);
        assertEquals("hai", repo.find("k").orElseThrow());
    }

    /* ---------- hoạt động người dùng ---------- */
    @Test
    void listenCounterIncrementsPerDayTargetAndLanguage() {
        ListenRepository repo = new ListenRepository(jdbc);
        repo.increment("2026-10-30", "site", 1, "vi");
        repo.increment("2026-10-30", "site", 1, "vi");
        repo.increment("2026-10-30", "site", 1, "en");
        repo.increment("2026-10-31", "site", 1, "vi");

        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM listens", Integer.class));
        assertEquals(2, jdbc.queryForObject(
                "SELECT count FROM listens WHERE date = '2026-10-30' AND language = 'vi'", Integer.class));
        assertEquals(4, repo.total("site", 1));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(DISTINCT id) FROM listens", Integer.class));
    }

    @Test
    void likesAreIdempotentPerDevice() {
        UserLikeRepository repo = new UserLikeRepository(jdbc);
        repo.add("dev-a", "site", 1, "Dinh", 100);
        repo.add("dev-a", "site", 1, "Dinh", 200);
        repo.add("dev-b", "site", 1, "Dinh", 300);
        List<LikeItem> likes = repo.list("dev-a");
        assertEquals(1, likes.size());
        assertEquals("Dinh", likes.get(0).name());
        assertEquals(1, repo.remove("dev-a", "site", 1));
        assertEquals(0, repo.remove("dev-a", "site", 1));
        assertEquals(1, repo.list("dev-b").size());
    }

    @Test
    void historyIsPerDeviceNewestFirstAndClearable() {
        UserHistoryRepository repo = new UserHistoryRepository(jdbc);
        repo.add("dev-a", "site", 1, "Một", "vi", "manual", 1000);
        repo.add("dev-a", "artifact", 2, "Hai", "en", "qr", 2000);
        repo.add("dev-b", "site", 1, "Khác", "vi", "manual", 3000);

        List<HistoryItem> items = repo.list("dev-a", 10);
        assertEquals(2, items.size());
        assertEquals("Hai", items.get(0).name());
        assertEquals(1, repo.list("dev-a", 1).size());
        assertEquals(2, repo.clear("dev-a"));
        assertTrue(repo.list("dev-a", 10).isEmpty());
        assertEquals(1, repo.list("dev-b", 10).size());
    }

    /* ---------- nội dung ---------- */
    private void insertSite(long id, String status, boolean hidden, String img, String extra) {
        jdbc.update("INSERT INTO sites (id, name, classify, city, address, description, img, extraImages, latitude, "
                        + "longitude, status, hidden, createdAt, updatedAt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, "Địa điểm " + id, "Di tích", "TP.HCM", "Địa chỉ", "Mô tả " + id, img, extra, 10.5, 106.5, status,
                hidden, "c", "u");
    }

    @Test
    void publishedSitesExcludeDraftsAndSkipHeavyColumns() {
        insertSite(1, "published", false, "data:image/png;base64,aGk=", "[\"data:image/png;base64,YQ==\",\"x\"]");
        insertSite(2, "draft", false, "", "[]");
        insertSite(3, "published", true, "", "[]");

        List<Site> sites = content.findPublishedSites();
        assertEquals(List.of(1L, 3L), sites.stream().map(Site::id).toList());
        Site first = sites.get(0);
        assertTrue(first.hasImage());
        assertTrue(first.extraImages().isEmpty(), "danh sách không được tải ảnh phụ");
        assertTrue(sites.get(1).hidden());
        assertFalse(sites.get(1).hasImage());

        Site detail = content.findPublishedSite(1, true).orElseThrow();
        assertEquals(2, detail.extraImages().size());
        assertTrue(content.findPublishedSite(2, false).isEmpty());
        assertEquals("data:image/png;base64,aGk=", content.findImage(TargetType.SITE, 1).orElseThrow());
        assertTrue(content.findImage(TargetType.SITE, 3).isEmpty());
        assertEquals(2, content.findExtraImages(1).size());
    }

    @Test
    void publishedArtifactsAreReadWithSiteId() {
        jdbc.update("INSERT INTO artifacts (id, siteId, name, address, description, img, latitude, longitude, status, hidden) "
                + "VALUES (7, 1, 'Xe tăng', 'đc', 'mô tả', 'data:image/png;base64,aGk=', 1.5, 2.5, 'published', FALSE)");
        List<Artifact> list = content.findPublishedArtifacts();
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).siteId());
        assertTrue(list.get(0).hasImage());
        assertEquals(2.5, content.findPublishedArtifact(7).orElseThrow().longitude());
    }

    @Test
    void narrationJsonConfigsAreParsedAndBadRowsSkipped() {
        String gps = "{\"enabled\":true,\"latitude\":10.777108,\"longitude\":106.695441,\"radius\":120}";
        String ble = "{\"enabled\":true,\"deviceName\":\"B1\",\"mac\":\"AA:BB\",\"radius\":5}";
        String qr = "{\"enabled\":true,\"code\":\"DD-1\",\"link\":\"\"}";
        jdbc.update("INSERT INTO narrations (id, targetType, targetId, text, useDescription, ttsEnabled, audioEnabled, "
                + "language, speed, audioName, audioData, gps, bluetooth, qr, translations) "
                + "VALUES (1,'site',1,'',TRUE,TRUE,TRUE,'vi',1.5,'a.mp3','data:audio/mpeg;base64,aGk=',?,?,?,?)",
                gps, ble, qr, "{\"EN\":\"Hello\",\"fr\":\"\"}");
        jdbc.update("INSERT INTO narrations (id, targetType, targetId) VALUES (2,'weird',1)");

        List<Narration> all = content.findAllNarrations();
        assertEquals(1, all.size(), "dòng có targetType lạ phải bị bỏ qua");
        Narration n = content.findNarration(TargetType.SITE, 1).orElseThrow();
        assertTrue(n.gps().enabled());
        assertEquals(120, n.gps().radius());
        assertEquals("AA:BB", n.bluetooth().mac());
        assertEquals("DD-1", n.qr().code());
        assertEquals(1.5, n.speed());
        assertTrue(n.hasAudio());
        assertEquals("Hello", n.translations().get("en"));
        assertFalse(n.translations().containsKey("fr"), "bản dịch rỗng bị bỏ");
        assertEquals("a.mp3", content.findAudio(TargetType.SITE, 1).orElseThrow().name());
        assertTrue(content.findNarration(TargetType.ARTIFACT, 1).isEmpty());
    }

    @Test
    void narrationWithMissingOrMalformedJsonFallsBackToDisabled() {
        jdbc.update("INSERT INTO narrations (id, targetType, targetId, gps, qr) VALUES (5,'site',5,'khong phai json',NULL)");
        Narration n = content.findNarration(TargetType.SITE, 5).orElseThrow();
        assertFalse(n.gps().enabled());
        assertFalse(n.qr().enabled());
        assertFalse(n.bluetooth().enabled());
        assertEquals("vi", n.language());
    }

    @Test
    void languagesAreReadWithEnabledFlag() {
        jdbc.update("INSERT INTO languages (id, name, code, ttsKey, enabled) VALUES (1,'Tiếng Việt','vi','vi-VN',TRUE)");
        jdbc.update("INSERT INTO languages (id, name, code, ttsKey, enabled) VALUES (2,'Khác','xx','xx',FALSE)");
        var langs = content.findLanguages();
        assertEquals(2, langs.size());
        assertTrue(langs.get(0).enabled());
        assertFalse(langs.get(1).enabled());
        Optional<String> none = content.findImage(TargetType.ARTIFACT, 99);
        assertTrue(none.isEmpty());
    }
}
