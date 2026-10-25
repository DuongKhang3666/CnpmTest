package backend.commentary.service;

import backend.commentary.dto.request.LoginRequest;
import backend.commentary.dto.response.AuthResponse;
import backend.commentary.entity.AdminUser;
import backend.commentary.repository.AdminUserRepository;
import backend.commentary.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Service xử lý đăng nhập và xác thực tài khoản quản trị
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository adminUserRepository;
    private final JwtUtils jwtUtils;
    private final AuditLogService auditLogService;

    public AuthService(AuthenticationManager authenticationManager,
                       AdminUserRepository adminUserRepository,
                       JwtUtils jwtUtils,
                       AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.adminUserRepository = adminUserRepository;
        this.jwtUtils = jwtUtils;
        this.auditLogService = auditLogService;
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword())
            );

            AdminUser admin = adminUserRepository.findByEmail(request.getEmail().trim().toLowerCase())
                    .orElseThrow(() -> new UsernameNotFoundException("Tài khoản quản trị không tồn tại."));

            String token = jwtUtils.generateToken(admin.getEmail(), admin.getRole());

            // Audit log login
            auditLogService.log(admin.getEmail(), "LOGIN", "ADMIN_USER", admin.getId(), admin.getEmail(),
                    Map.of("loginSuccess", true), httpRequest);

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .id(admin.getId())
                    .email(admin.getEmail())
                    .role(admin.getRole())
                    .build();

        } catch (BadCredentialsException e) {
            // Audit log failed login
            auditLogService.logSimple(request.getEmail().trim().toLowerCase(), "LOGIN_FAILED", "ADMIN_USER", null, null,
                    Map.of("reason", "Invalid credentials"));
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
        }
    }
}
