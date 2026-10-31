package vn.commentary.backend.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.commentary.backend.user.dto.Dtos.DeviceInfo;
import vn.commentary.backend.user.dto.Dtos.JoinRequest;
import vn.commentary.backend.user.dto.Dtos.JoinResponse;
import vn.commentary.backend.user.exception.UserAccessException;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.repository.DeviceSessionRepository;
import vn.commentary.backend.user.repository.SettingsRepository;
import vn.commentary.backend.common.util.TokenHasher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionServiceTest {
    private static final long NOW = 1_700_000_000_000L;
    private static final String DEVICE = "device-12345678";

    private DeviceSessionRepository sessions;
    private AccessTokenService tokens;
    private SettingsRepository settings;
    private SessionService service;

    @BeforeEach
    void setUp() {
        sessions = mock(DeviceSessionRepository.class);
        tokens = mock(AccessTokenService.class);
        settings = mock(SettingsRepository.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC);
        service = new SessionService(sessions, tokens, settings, clock, 45, 24, 5, false);
        when(sessions.find(anyString())).thenReturn(Optional.empty());
        when(settings.accessHours(anyInt())).thenReturn(24);
    }

    private static JoinRequest web(String token) {
        return new JoinRequest(token, DEVICE, null, new DeviceInfo("Mozilla/5.0 (Windows NT 10.0) Chrome/150",
                "Win32", "vi", "1920x1080", "Asia/Saigon", null, "web"));
    }

    private DeviceSession existing(long expiresAt, String rawToken) {
        return new DeviceSession(DEVICE, TokenHasher.sha256(rawToken), "Windows", "Chrome", "web", "10.0.0.2", "", "",
                "", "", "", NOW - 10_000, NOW - 1_000, expiresAt, 24);
    }

    @Test
    void joinWithValidQrCreatesSession() {
        when(tokens.matches("qr-token")).thenReturn(true);

        JoinResponse response = service.join(web("qr-token"), "192.168.1.5");

        assertTrue(response.ok());
        assertEquals(64, response.sessionToken().length());
        ArgumentCaptor<DeviceSession> saved = ArgumentCaptor.forClass(DeviceSession.class);
        verify(sessions).save(saved.capture());
        assertEquals(TokenHasher.sha256(response.sessionToken()), saved.getValue().sessionHash());
        assertEquals(NOW + 24 * 3_600_000L, saved.getValue().expiresAt());
        assertEquals("Windows", saved.getValue().name());
        assertEquals("web", saved.getValue().clientType());
    }

    @Test
    void joinRejectsBadDeviceId() {
        UserAccessException ex = assertThrows(UserAccessException.class,
                () -> service.join(new JoinRequest("x", "a b", null, null), "1.1.1.1"));
        assertEquals("bad-device", ex.getReason());
        assertThrows(UserAccessException.class, () -> service.join(null, "1.1.1.1"));
    }

    @Test
    void joinWithoutOrWithWrongTokenIsRejected() {
        when(tokens.matches(anyString())).thenReturn(false);
        assertEquals("no-token", assertThrows(UserAccessException.class, () -> service.join(web(null), "ip")).getReason());
        assertEquals("token-invalid", assertThrows(UserAccessException.class, () -> service.join(web("sai"), "ip")).getReason());
        verify(sessions, never()).save(any());
    }

    @Test
    void expiredDeviceCannotJoinEvenWithValidQr() {
        when(tokens.matches(anyString())).thenReturn(true);
        when(sessions.find(DEVICE)).thenReturn(Optional.of(existing(NOW - 1, "old")));
        assertEquals("expired", assertThrows(UserAccessException.class, () -> service.join(web("qr"), "ip")).getReason());
    }

    @Test
    void knownDeviceCanRejoinWithSessionTokenAndKeepsExpiry() {
        long expiry = NOW + 5_000_000L;
        when(sessions.find(DEVICE)).thenReturn(Optional.of(existing(expiry, "old-session")));
        JoinRequest request = new JoinRequest(null, DEVICE, "old-session", null);

        JoinResponse response = service.join(request, "ip");

        // Giữ nguyên token để các lời gọi vào lại đồng thời (nhiều tab) không làm lệch token.
        assertEquals("old-session", response.sessionToken());
        ArgumentCaptor<DeviceSession> saved = ArgumentCaptor.forClass(DeviceSession.class);
        verify(sessions).save(saved.capture());
        assertEquals(TokenHasher.sha256("old-session"), saved.getValue().sessionHash());
        assertEquals(expiry, saved.getValue().expiresAt());
        assertEquals(NOW - 10_000, saved.getValue().firstSeenAt());
    }

    @Test
    void knowingOnlyDeviceIdIsNotEnough() {
        when(sessions.find(DEVICE)).thenReturn(Optional.of(existing(NOW + 5_000_000L, "old-session")));
        when(tokens.matches(any())).thenReturn(false);
        assertThrows(UserAccessException.class, () -> service.join(new JoinRequest(null, DEVICE, "doan-bua", null), "ip"));
    }

    @Test
    void appDeviceLimitIsEnforced() {
        when(tokens.matches("qr")).thenReturn(true);
        when(settings.maxAppDevices(anyInt())).thenReturn(2);
        when(sessions.countOnlineApps(anyLong(), anyLong(), anyString())).thenReturn(2);
        JoinRequest app = new JoinRequest("qr", DEVICE, null,
                new DeviceInfo("okhttp", "android", "vi", "", "", "Pixel", "app"));

        UserAccessException ex = assertThrows(UserAccessException.class, () -> service.join(app, "ip"));

        assertEquals("app-limit", ex.getReason());
        verify(sessions, never()).save(any());
    }

    @Test
    void authenticateAcceptsValidSessionAndRefreshesLastSeen() {
        DeviceSession stale = new DeviceSession(DEVICE, TokenHasher.sha256("tok"), "Windows", "Chrome", "web", "ip",
                "", "", "", "", "", NOW - 100_000, NOW - 60_000, NOW + 1_000_000L, 24);
        when(sessions.find(DEVICE)).thenReturn(Optional.of(stale));
        DeviceSession s = service.authenticate(DEVICE, "tok");
        assertEquals(DEVICE, s.deviceId());
        verify(sessions).setLastSeen(DEVICE, NOW);
    }

    @Test
    void authenticateRejectsMissingWrongAndExpired() {
        assertEquals("no-session", assertThrows(UserAccessException.class, () -> service.authenticate(null, null)).getReason());
        assertEquals("no-session", assertThrows(UserAccessException.class, () -> service.authenticate(DEVICE, "tok")).getReason());

        when(sessions.find(DEVICE)).thenReturn(Optional.of(existing(NOW + 1_000_000L, "tok")));
        assertEquals("invalid-session", assertThrows(UserAccessException.class, () -> service.authenticate(DEVICE, "sai")).getReason());

        when(sessions.find(DEVICE)).thenReturn(Optional.of(existing(NOW - 1, "tok")));
        assertEquals("expired", assertThrows(UserAccessException.class, () -> service.authenticate(DEVICE, "tok")).getReason());
    }

    @Test
    void statusReportsRemainingSeconds() {
        DeviceSession s = existing(NOW + 90_000L, "tok");
        assertEquals(90, service.status(s).remainingSeconds());
    }

    @Test
    void lanOnlyRejectsPublicClientsButAllowsPrivateOnes() {
        SessionService lan = new SessionService(sessions, tokens, settings,
                Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC), 45, 24, 5, true);
        when(tokens.matches("qr-token")).thenReturn(true);

        UserAccessException ex = assertThrows(UserAccessException.class,
                () -> lan.join(web("qr-token"), "203.0.113.9"));
        assertEquals("not-lan", ex.getReason());
        assertTrue(lan.join(web("qr-token"), "192.168.1.5").ok());
        assertTrue(lan.join(web("qr-token"), "127.0.0.1").ok());
    }
}
