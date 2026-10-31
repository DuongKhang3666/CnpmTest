package vn.commentary.backend.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Bean dùng chung. Tách riêng khỏi UserWebConfig để tránh vòng phụ thuộc với các interceptor. */
@Configuration
public class UserConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
