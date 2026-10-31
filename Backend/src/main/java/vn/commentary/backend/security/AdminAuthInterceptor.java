package vn.commentary.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import vn.commentary.backend.service.AdminAuthService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Cổng bảo vệ mặc định cho API quản trị: mọi đường dẫn đều cần đăng nhập, trừ những đường được loại ra
 * khi đăng ký interceptor (API người dùng có cơ chế phiên riêng, và endpoint đăng nhập).
 * Chấp nhận {@code Authorization: Bearer <token>} hoặc {@code X-Admin-Key} (nếu có cấu hình ADMIN_API_KEY).
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    public static final String ATTR_EMAIL = "adminEmail";
    public static final String HEADER_KEY = "X-Admin-Key";

    private final AdminAuthService auth;

    public AdminAuthInterceptor(AdminAuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // preflight CORS đã được CorsFilter trả lời
        Optional<String> who = Optional.empty();
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            who = auth.authenticateToken(header.substring(7).trim());
        }
        if (who.isEmpty() && auth.matchesApiKey(request.getHeader(HEADER_KEY))) who = Optional.of("api-key");
        if (who.isPresent()) {
            request.setAttribute(ATTR_EMAIL, who.get());
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("WWW-Authenticate", "Bearer");
        String msg = "Cần đăng nhập quản trị để thực hiện thao tác này.";
        response.getWriter().write("{\"ok\":false,\"reason\":\"admin-auth\",\"message\":\"" + msg + "\",\"error\":\"" + msg + "\"}");
        return false;
    }
}
