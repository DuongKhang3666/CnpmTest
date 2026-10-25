package backend.commentary.config;

import backend.commentary.entity.AdminUser;
import backend.commentary.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Tự động khởi tạo (seed) tài khoản Admin mặc định khi khởi động ứng dụng nếu chưa tồn tại
 */
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@gmail.com}")
    private String adminEmail;

    @Value("${app.admin.password:123456}")
    private String adminPassword;

    public AdminUserInitializer(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String cleanEmail = adminEmail.trim().toLowerCase();

        if (!adminUserRepository.existsByEmail(cleanEmail)) {
            AdminUser admin = AdminUser.builder()
                    .email(cleanEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role("ROLE_ADMIN")
                    .build();

            adminUserRepository.save(admin);
            logger.info(">>> Đã khởi tạo thành công tài khoản quản trị mặc định: {}", cleanEmail);
        } else {
            logger.info(">>> Tài khoản quản trị đã tồn tại: {}", cleanEmail);
        }
    }
}
