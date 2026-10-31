package vn.commentary.backend.admin.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginThrottleTest {
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-30T10:00:00Z");
        public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    @Test
    void locksAfterMaxFailuresAndReleasesAfterWindow() {
        MutableClock clock = new MutableClock();
        LoginThrottle t = new LoginThrottle(3, 300, clock);
        assertEquals(0, t.retryAfterSeconds("ip"));
        t.recordFailure("ip");
        t.recordFailure("ip");
        assertEquals(0, t.retryAfterSeconds("ip"));
        t.recordFailure("ip");
        assertEquals(300, t.retryAfterSeconds("ip"));
        assertEquals(0, t.retryAfterSeconds("other-ip"));
        clock.now = clock.now.plusSeconds(299);
        assertEquals(1, t.retryAfterSeconds("ip"));
        clock.now = clock.now.plusSeconds(1);
        assertEquals(0, t.retryAfterSeconds("ip"));
    }

    @Test
    void resetClearsFailures() {
        LoginThrottle t = new LoginThrottle(2, 300, new MutableClock());
        t.recordFailure("ip");
        t.recordFailure("ip");
        assertTrue(t.retryAfterSeconds("ip") > 0);
        t.reset("ip");
        assertEquals(0, t.retryAfterSeconds("ip"));
    }
}
