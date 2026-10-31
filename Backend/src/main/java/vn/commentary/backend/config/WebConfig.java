package vn.commentary.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.commentary.backend.security.AdminAuthInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình web chung:
 * <ul>
 *   <li>Bảo vệ mặc định: mọi đường dẫn cần đăng nhập admin, trừ API người dùng (có phiên thiết bị riêng) và endpoint đăng nhập.</li>
 *   <li>CORS: chỉ cho trang web từ mạng nội bộ/localhost hoặc origin được liệt kê trong {@code CORS_ALLOWED_ORIGINS}.</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AdminAuthInterceptor adminAuth;

    public WebConfig(AdminAuthInterceptor adminAuth) {
        this.adminAuth = adminAuth;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuth)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/user/**", "/api/admin/auth/login", "/error");
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(@Value("${cors.allowed-origins:}") String allowedOrigins) {
        List<String> extra = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        CorsConfiguration config = new LanOnlyCorsConfiguration(extra);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
