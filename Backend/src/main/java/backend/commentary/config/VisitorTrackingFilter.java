package backend.commentary.config;

import backend.commentary.service.VisitorTrackingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter ghi nhận lượt truy cập: tự động chạy 1 lần cho mỗi request API
 * Bỏ qua các request tĩnh (uploads, swagger, h2-console...) và request OPTIONS (CORS preflight)
 */
@Component
public class VisitorTrackingFilter extends OncePerRequestFilter {

    private final VisitorTrackingService visitorTrackingService;

    public VisitorTrackingFilter(VisitorTrackingService visitorTrackingService) {
        this.visitorTrackingService = visitorTrackingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        visitorTrackingService.recordVisit(request);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !uri.startsWith("/api/")
                || uri.startsWith("/api/admin/audit-logs")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui");
    }
}
