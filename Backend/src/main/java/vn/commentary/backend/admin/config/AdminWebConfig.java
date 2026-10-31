package vn.commentary.backend.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.commentary.backend.admin.security.AdminAuthInterceptor;

/**
 * Bảo vệ mặc định cho phía quản trị: mọi đường dẫn cần đăng nhập admin, trừ API người dùng
 * (có phiên thiết bị riêng, xem {@code UserWebConfig}) và endpoint đăng nhập.
 */
@Configuration
public class AdminWebConfig implements WebMvcConfigurer {
    private final AdminAuthInterceptor adminAuth;

    public AdminWebConfig(AdminAuthInterceptor adminAuth) {
        this.adminAuth = adminAuth;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuth)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/user/**", "/api/admin/auth/login", "/error");
    }
}
