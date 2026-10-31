package vn.commentary.backend.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.commentary.backend.user.security.SessionInterceptor;

@Configuration
public class UserWebConfig implements WebMvcConfigurer {
    private final SessionInterceptor sessionInterceptor;

    public UserWebConfig(SessionInterceptor sessionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/api/user/**")
                // vào hệ thống và health là công khai; ảnh/audio phải nhúng được vào <img>/<audio> (không gửi được header)
                .excludePathPatterns(
                        "/api/user/health",
                        "/api/user/session/join",
                        "/api/user/sites/*/image",
                        "/api/user/sites/*/images/*",
                        "/api/user/artifacts/*/image",
                        "/api/user/narrations/*/*/audio");
    }
}
