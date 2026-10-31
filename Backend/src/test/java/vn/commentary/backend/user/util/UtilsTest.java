package vn.commentary.backend.user.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
    @Test
    void haversineGivesPlausibleDistance() {
        double d = GeoUtils.distanceMeters(10.777108, 106.695441, 10.779239, 106.692391);
        assertTrue(d > 300 && d < 500, "khoảng cách thực tế khoảng 400m, nhận " + d);
        assertEquals(0, GeoUtils.distanceMeters(10, 106, 10, 106), 0.001);
    }

    @Test
    void latLngValidation() {
        assertFalse(GeoUtils.isValidLatLng(91, 0));
        assertFalse(GeoUtils.isValidLatLng(0, 181));
        assertFalse(GeoUtils.isValidLatLng(Double.NaN, 0));
        assertTrue(GeoUtils.isValidLatLng(10.7, 106.6));
    }

    @Test
    void foldRemovesVietnameseDiacritics() {
        assertEquals("dinh doc lap", TextNormalizer.fold("Dinh Độc Lập"));
        assertEquals("dong khoi", TextNormalizer.fold("  ĐỒNG Khởi "));
        assertEquals("", TextNormalizer.fold(null));
    }

    @Test
    void macIsNormalized() {
        assertEquals("AABBCCDDEEFF", TextNormalizer.normalizeMac("aa:bb-cc dd:ee:ff"));
    }

    @Test
    void chunkerKeepsEveryPieceWithinLimit() {
        assertEquals(List.of("abc"), TextChunker.split("abc", 10));
        assertTrue(TextChunker.split("", 10).isEmpty());
        List<String> chunks = TextChunker.split("aaaa bbbb cccc dddd", 9);
        assertTrue(chunks.stream().allMatch(s -> s.length() <= 9));
        assertEquals("aaaa bbbb cccc dddd", String.join(" ", chunks).replace("\n", " "));
        String big = "x".repeat(25);
        assertEquals(big, String.join("", TextChunker.split(big, 10)));
    }

    @Test
    void dataUrlParsing() {
        DataUrl.Parsed p = DataUrl.parse("data:image/png;base64,aGVsbG8=");
        assertEquals("image/png", p.mime());
        assertEquals("hello", new String(p.bytes()));
        assertEquals("audio/mpeg", DataUrl.parse("data:audio/mpeg;codecs=x;base64,aGk=").mime());
        assertEquals("hi there", new String(DataUrl.parse("data:text/plain,hi%20there").bytes()));
        assertFalse(DataUrl.isDataUrl("http://x"));
        assertThrows(IllegalArgumentException.class, () -> DataUrl.parse("khong-phai-data-url"));
    }

    @Test
    void tokenHashing() {
        String token = TokenHasher.newSessionToken();
        assertEquals(64, token.length());
        assertEquals(32, TokenHasher.newQrToken().length());
        String hash = TokenHasher.sha256(token);
        assertTrue(TokenHasher.matchesHash(token, hash));
        assertFalse(TokenHasher.matchesHash("khac", hash));
        assertFalse(TokenHasher.matchesHash(null, hash));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", TokenHasher.sha256("abc"));
        assertTrue(TokenHasher.safeEquals("a", "a"));
        assertFalse(TokenHasher.safeEquals("a", null));
    }

    @Test
    void deviceDescriber() {
        DeviceDescriber.Description android = DeviceDescriber.describe(
                "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit Chrome/120 Mobile Safari/537.36");
        assertEquals("SM-S918B", android.name());
        assertEquals("Chrome", android.browser());
        DeviceDescriber.Description win = DeviceDescriber.describe("Mozilla/5.0 (Windows NT 10.0) Chrome/156 Safari/537 Edg/156");
        assertEquals("Windows", win.name());
        assertEquals("Edge", win.browser());
        assertEquals("Thiết bị", DeviceDescriber.describe(null).name());
    }
}
