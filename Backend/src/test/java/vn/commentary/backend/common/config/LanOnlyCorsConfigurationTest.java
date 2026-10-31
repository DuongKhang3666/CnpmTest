package vn.commentary.backend.common.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LanOnlyCorsConfigurationTest {
    private final LanOnlyCorsConfiguration lan = new LanOnlyCorsConfiguration(List.of());

    @Test
    void allowsLocalAndPrivateNetworkOrigins() {
        for (String o : new String[]{"http://localhost:5173", "http://127.0.0.1:5173", "http://192.168.1.10:5173",
                "http://10.0.0.5", "http://172.20.1.1:8080", "https://192.168.0.2", "http://[::1]:5173"}) {
            assertEquals(o, lan.checkOrigin(o), o);
        }
    }

    @Test
    void rejectsPublicAndLookalikeOrigins() {
        for (String o : new String[]{"https://evil.com", "http://192.168.1.10.evil.com", "http://10.evil.com",
                "http://8.8.8.8", "null", "file://x", "javascript:alert(1)", "http://"}) {
            assertNull(lan.checkOrigin(o), o);
        }
        assertNull(lan.checkOrigin(null));
    }

    @Test
    void listedOriginsAndWildcard() {
        LanOnlyCorsConfiguration c = new LanOnlyCorsConfiguration(List.of("https://admin.example.com/"));
        assertEquals("https://admin.example.com", c.checkOrigin("https://admin.example.com"));
        assertNull(c.checkOrigin("https://other.example.com"));
        assertEquals("https://x.y", new LanOnlyCorsConfiguration(List.of("*")).checkOrigin("https://x.y"));
    }
}
