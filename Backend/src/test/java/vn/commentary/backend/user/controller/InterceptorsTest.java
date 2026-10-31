package vn.commentary.backend.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vn.commentary.backend.user.exception.UserAccessException;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.security.SessionInterceptor;
import vn.commentary.backend.user.service.SessionService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterceptorsTest {
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void sessionInterceptorStoresAuthenticatedSession() {
        SessionService service = mock(SessionService.class);
        DeviceSession session = new DeviceSession("device-12345678", "h", "W", "C", "web", "ip", "", "", "", "", "",
                0, 0, 0, 24);
        when(service.authenticate("device-12345678", "tok")).thenReturn(session);
        SessionInterceptor interceptor = new SessionInterceptor(service);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/sites");
        request.addHeader(SessionInterceptor.HEADER_DEVICE, "device-12345678");
        request.addHeader(SessionInterceptor.HEADER_TOKEN, "tok");

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertSame(session, request.getAttribute(SessionInterceptor.ATTR));
    }

    @Test
    void sessionInterceptorPropagatesAccessErrorsAndSkipsPreflight() {
        SessionService service = mock(SessionService.class);
        when(service.authenticate(any(), any())).thenThrow(new UserAccessException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "no-session", "x"));
        SessionInterceptor interceptor = new SessionInterceptor(service);

        assertThrows(UserAccessException.class,
                () -> interceptor.preHandle(new MockHttpServletRequest("GET", "/api/user/sites"), response, new Object()));
        assertTrue(interceptor.preHandle(new MockHttpServletRequest("OPTIONS", "/api/user/sites"), response, new Object()));
        verify(service, times(1)).authenticate(any(), any());
    }
}
