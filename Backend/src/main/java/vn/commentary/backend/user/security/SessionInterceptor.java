package vn.commentary.backend.user.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import vn.commentary.backend.user.service.SessionService;

/**
 * Chặn mọi lời gọi {@code /api/user/**} (trừ vào hệ thống, health, ảnh/audio công khai) nếu thiết bị chưa
 * có phiên hợp lệ. Client gửi header {@code X-Device-Id} và {@code X-Session-Token}.
 */
@Component
public class SessionInterceptor implements HandlerInterceptor {
    public static final String ATTR = "vn.commentary.deviceSession";
    public static final String HEADER_DEVICE = "X-Device-Id";
    public static final String HEADER_TOKEN = "X-Session-Token";

    private final SessionService sessionService;

    public SessionInterceptor(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // CORS preflight
        request.setAttribute(ATTR, sessionService.authenticate(
                request.getHeader(HEADER_DEVICE), request.getHeader(HEADER_TOKEN)));
        return true;
    }
}
