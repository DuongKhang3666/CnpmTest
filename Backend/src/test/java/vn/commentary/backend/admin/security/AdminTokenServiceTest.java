package vn.commentary.backend.admin.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class AdminTokenServiceTest {
    private static final String SECRET = "0123456789abcdef0123";
    private final Clock now = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuedTokenVerifies() {
        AdminTokenService s = new AdminTokenService(SECRET, 3600, now);
        var claims = s.verify(s.issue("a@b.c"));
        assertTrue(claims.isPresent());
        assertEquals("a@b.c", claims.get().email());
        assertEquals(now.instant().getEpochSecond() + 3600, claims.get().expiresAtEpochSec());
    }

    @Test
    void expiredTokenIsRejected() {
        String token = new AdminTokenService(SECRET, 60, now).issue("a@b.c");
        Clock later = Clock.fixed(now.instant().plusSeconds(61), ZoneOffset.UTC);
        assertTrue(new AdminTokenService(SECRET, 60, later).verify(token).isEmpty());
    }

    @Test
    void tamperedOrForeignTokensAreRejected() {
        AdminTokenService s = new AdminTokenService(SECRET, 3600, now);
        String token = s.issue("a@b.c");
        String[] parts = token.split("\\.");
        // đổi payload (email khác) nhưng giữ chữ ký cũ
        String forged = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("evil@x.y|" + (now.instant().getEpochSecond() + 99999)).getBytes()) + "." + parts[1];
        assertTrue(s.verify(forged).isEmpty());
        assertTrue(s.verify(token + "x").isEmpty());
        // đổi ký tự cuối của chữ ký (bit thừa của base64) cũng không được chấp nhận
        char last = token.charAt(token.length() - 1);
        for (char c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray()) {
            String mutated = token.substring(0, token.length() - 1) + c;
            assertEquals(c == last, s.verify(mutated).isPresent(), "ký tự cuối = " + c);
        }
        assertTrue(new AdminTokenService("another-secret-0000000", 3600, now).verify(token).isEmpty());
        for (String bad : new String[]{null, "", ".", "abc", "a.b", "....", "!!!.???"}) {
            assertTrue(s.verify(bad).isEmpty(), "phải từ chối: " + bad);
        }
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new AdminTokenService("short", 60, now));
    }
}
