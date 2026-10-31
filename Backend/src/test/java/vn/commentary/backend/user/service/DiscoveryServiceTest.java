package vn.commentary.backend.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.commentary.backend.common.exception.ResourceNotFoundException;
import vn.commentary.backend.user.dto.Dtos.NearbyMatch;
import vn.commentary.backend.user.dto.Dtos.ResolveResult;
import vn.commentary.backend.user.model.Models.BeaconConfig;
import vn.commentary.backend.user.model.Models.GeoConfig;
import vn.commentary.backend.user.model.Models.Narration;
import vn.commentary.backend.user.model.Models.QrConfig;
import vn.commentary.backend.user.model.Models.TargetInfo;
import vn.commentary.backend.user.model.Models.TargetType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscoveryServiceTest {
    private ContentService content;
    private DiscoveryService service;

    private static Narration narration(long id, TargetType type, long targetId, GeoConfig gps, BeaconConfig ble,
                                       QrConfig qr) {
        return new Narration(id, type, targetId, "", true, true, false, "", false, "vi", 1.0, gps, ble, qr, Map.of());
    }

    @BeforeEach
    void setUp() {
        content = mock(ContentService.class);
        service = new DiscoveryService(content);
        when(content.visibleTargetIndex()).thenReturn(Map.of(
                "site:1", new TargetInfo(TargetType.SITE, 1, "Dinh Độc Lập", "mô tả", null, null),
                "site:2", new TargetInfo(TargetType.SITE, 2, "Bảo tàng", "mô tả", null, null),
                "artifact:3", new TargetInfo(TargetType.ARTIFACT, 3, "Xe tăng", "mô tả", 2L, "Bảo tàng")));
        when(content.narrations()).thenReturn(List.of(
                narration(1, TargetType.SITE, 1, new GeoConfig(true, 10.777108, 106.695441, 120),
                        new BeaconConfig(true, "B1", "AA:BB:CC:DD:EE:01", 5), new QrConfig(true, "DD-1", "")),
                narration(2, TargetType.SITE, 2, new GeoConfig(true, 10.779239, 106.692391, 50),
                        BeaconConfig.disabled(), new QrConfig(true, "DD-2", "https://app/play?type=site&id=2")),
                narration(3, TargetType.ARTIFACT, 3, GeoConfig.disabled(), BeaconConfig.disabled(),
                        new QrConfig(false, "HV-3", "")),
                narration(9, TargetType.SITE, 9, new GeoConfig(true, 10.777108, 106.695441, 500),
                        BeaconConfig.disabled(), new QrConfig(true, "AN", "")))); // site 9 không hiển thị
    }

    @Test
    void resolvesQrByCodeIgnoringCase() {
        ResolveResult r = service.resolveQr("  dd-1 ");
        assertEquals("site", r.targetType());
        assertEquals(1, r.targetId());
        assertEquals("qr", r.via());
    }

    @Test
    void resolvesQrByStoredLinkAndByPlayLink() {
        assertEquals(2, service.resolveQr("https://app/play?type=site&id=2").targetId());
        ResolveResult viaParam = service.resolveQr("https://other/play?type=artifact&id=3&lang=en&autoplay=1");
        assertEquals("artifact", viaParam.targetType());
        assertEquals(3, viaParam.targetId());
    }

    @Test
    void disabledOrUnknownOrHiddenQrIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.resolveQr("HV-3"));
        assertThrows(ResourceNotFoundException.class, () -> service.resolveQr("khong-co"));
        assertThrows(ResourceNotFoundException.class, () -> service.resolveQr("AN"));
        assertThrows(IllegalArgumentException.class, () -> service.resolveQr("   "));
    }

    @Test
    void nearbyReturnsOnlyTargetsInsideRadiusClosestFirst() {
        List<NearbyMatch> inside = service.nearby(10.777200, 106.695400);
        assertEquals(1, inside.size());
        assertEquals(1, inside.get(0).targetId());
        assertTrue(inside.get(0).distanceMeters() < 120);

        assertTrue(service.nearby(10.9, 106.9).isEmpty());
    }

    @Test
    void nearbyRejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> service.nearby(120, 0));
    }

    @Test
    void resolvesBeaconByNormalizedMac() {
        ResolveResult r = service.resolveBeacon("aa-bb-cc-dd-ee-01");
        assertEquals(1, r.targetId());
        assertEquals("beacon", r.via());
        assertThrows(ResourceNotFoundException.class, () -> service.resolveBeacon("00:00:00:00:00:00"));
        assertThrows(IllegalArgumentException.class, () -> service.resolveBeacon(" "));
    }
}
