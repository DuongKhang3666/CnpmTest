package vn.commentary.backend.user.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.commentary.backend.user.service.AccessTokenService;

/** Khi khởi động: bảo đảm có sẵn một token QR hiện hành. */
@Component
public class UserBootstrap implements ApplicationRunner {
    private final AccessTokenService accessTokens;

    public UserBootstrap(AccessTokenService accessTokens) {
        this.accessTokens = accessTokens;
    }

    @Override
    public void run(ApplicationArguments args) {
        accessTokens.current();
    }
}
