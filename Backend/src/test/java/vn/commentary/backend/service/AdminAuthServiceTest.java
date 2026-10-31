package vn.commentary.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import vn.commentary.backend.security.AdminAuthException;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuthServiceTest {
    private AdminAuthService svc(String password, String apiKey) {
        return new AdminAuthService("Admin@Example.com", password, "0123456789abcdef0123", 12, apiKey, 3, 300, Clock.systemUTC());
    }

    @Test
    void loginSucceedsAndTokenAuthenticates() {
        AdminAuthService s = svc("pw-123456", "");
        var r = s.login(" admin@example.COM ", "pw-123456", "1.1.1.1");
        assertEquals("admin@example.com", r.email());
        assertEquals("admin@example.com", s.authenticateToken(r.token()).orElseThrow());
        assertTrue(s.authenticateToken("garbage").isEmpty());
    }

    @Test
    void wrongCredentialsAreRejectedWithSameReason() {
        AdminAuthService s = svc("pw-123456", "");
        AdminAuthException badPass = assertThrows(AdminAuthException.class, () -> s.login("admin@example.com", "x", "ip"));
        AdminAuthException badMail = assertThrows(AdminAuthException.class, () -> s.login("other@example.com", "pw-123456", "ip"));
        assertEquals(HttpStatus.UNAUTHORIZED, badPass.getStatus());
        assertEquals("bad-credentials", badPass.getReason());
        assertEquals(badPass.getMessage(), badMail.getMessage());
        assertThrows(AdminAuthException.class, () -> s.login(null, null, "ip"));
    }

    @Test
    void oldHardCodedAccountDoesNotWork() {
        AdminAuthService s = new AdminAuthService("admin@gmail.com", "S0methingElse!", "", 12, "", 5, 300, Clock.systemUTC());
        assertThrows(AdminAuthException.class, () -> s.login("admin@gmail.com", "123456", "ip"));
    }

    @Test
    void locksOutAfterTooManyFailuresEvenWithCorrectPassword() {
        AdminAuthService s = svc("pw-123456", "");
        for (int i = 0; i < 3; i++) assertThrows(AdminAuthException.class, () -> s.login("admin@example.com", "bad", "9.9.9.9"));
        AdminAuthException locked = assertThrows(AdminAuthException.class, () -> s.login("admin@example.com", "pw-123456", "9.9.9.9"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, locked.getStatus());
        assertEquals("too-many-attempts", locked.getReason());
        assertNotNull(s.login("admin@example.com", "pw-123456", "8.8.8.8")); // IP khác không bị ảnh hưởng
    }

    @Test
    void emptyPasswordConfigGeneratesRandomOneThatNeverMatchesEmptyInput() {
        AdminAuthService s = svc("", "");
        assertThrows(AdminAuthException.class, () -> s.login("admin@example.com", "", "ip"));
        assertThrows(AdminAuthException.class, () -> s.login("admin@example.com", null, "ip"));
    }

    @Test
    void apiKeyOnlyWorksWhenConfigured() {
        assertFalse(svc("p", "").matchesApiKey(""));
        assertFalse(svc("p", "").matchesApiKey(null));
        AdminAuthService s = svc("p", "k-secret");
        assertTrue(s.matchesApiKey("k-secret"));
        assertFalse(s.matchesApiKey("k-other"));
    }
}
