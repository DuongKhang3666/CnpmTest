package vn.commentary.backend.user.service;

import org.junit.jupiter.api.Test;
import vn.commentary.backend.user.dto.Dtos.Catalog;
import vn.commentary.backend.user.model.Models.Artifact;
import vn.commentary.backend.user.model.Models.BeaconConfig;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.model.Models.GeoConfig;
import vn.commentary.backend.user.model.Models.Language;
import vn.commentary.backend.user.model.Models.Narration;
import vn.commentary.backend.user.model.Models.QrConfig;
import vn.commentary.backend.user.model.Models.Site;
import vn.commentary.backend.user.model.Models.TargetInfo;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.repository.ListenRepository;
import vn.commentary.backend.user.repository.ListenRepository.TargetTotal;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogServiceTest {
    private static final DeviceSession SESSION = new DeviceSession("device-12345678", "h", "W", "C", "web", "ip",
            "", "", "", "", "", 0, 0, 0, 24);

    private static Narration narration(TargetType type, long id, boolean audio) {
        return new Narration(1, type, id, "nội dung", false, true, audio, "a.mp3", audio, "vi", 1.25,
                new GeoConfig(true, 10.77, 106.69, 120), BeaconConfig.disabled(), new QrConfig(true, "DD-1", ""),
                Map.of());
    }

    @Test
    void buildsCatalogWithOnlyVisibleNarrationsAndListens() {
        ContentService content = mock(ContentService.class);
        ActivityService activity = mock(ActivityService.class);
        ListenRepository listens = mock(ListenRepository.class);

        when(content.enabledLanguages()).thenReturn(List.of(new Language(1, "Tiếng Việt", "vi", "vi-VN", true)));
        when(content.sites(any(), any())).thenReturn(List.of(
                new Site(1, "Dinh Độc Lập", "Di tích", "HCM", "135 NKKN", "mô tả", 10.77, 106.69, true,
                        List.of(), false, "")));
        when(content.artifacts(any(), any())).thenReturn(List.of(
                new Artifact(5, 1, "Xe tăng", "", "mô tả", null, null, false, false, "")));
        when(content.visibleTargetIndex()).thenReturn(Map.of(
                "site:1", new TargetInfo(TargetType.SITE, 1, "Dinh Độc Lập", "", null, null),
                "artifact:5", new TargetInfo(TargetType.ARTIFACT, 5, "Xe tăng", "", 1L, "Dinh Độc Lập")));
        when(content.narrations()).thenReturn(List.of(
                narration(TargetType.SITE, 1, true),
                narration(TargetType.SITE, 99, false)));
        when(listens.totalsByTarget()).thenReturn(List.of(
                new TargetTotal("site", 1, 12), new TargetTotal("site", 99, 3)));

        Catalog catalog = new CatalogService(content, activity, listens).build(SESSION);

        assertEquals(1, catalog.languages().size());
        assertEquals("/api/user/sites/1/image", catalog.sites().get(0).imageUrl());
        assertEquals(1, catalog.sites().get(0).artifactCount());
        assertEquals("Dinh Độc Lập", catalog.artifacts().get(0).siteName());
        assertNull(catalog.artifacts().get(0).imageUrl());
        assertEquals(1, catalog.narrations().size());
        assertEquals("/api/user/narrations/site/1/audio", catalog.narrations().get(0).audioUrl());
        assertEquals("DD-1", catalog.narrations().get(0).qr().code());
        assertEquals(1.25, catalog.narrations().get(0).speed());
        assertEquals(1, catalog.listens().size());
        assertEquals(12, catalog.listens().get(0).count());
    }

    @Test
    void historyMethodLabelsAreNormalized() {
        assertEquals("qr", ActivityService.normalizeMethod("QR code"));
        assertEquals("gps", ActivityService.normalizeMethod("GPS"));
        assertEquals("beacon", ActivityService.normalizeMethod("Bluetooth"));
        assertEquals("auto", ActivityService.normalizeMethod("Hàng đợi"));
        assertEquals("manual", ActivityService.normalizeMethod(null));
        assertEquals("manual", ActivityService.normalizeMethod("khác"));
    }
}
