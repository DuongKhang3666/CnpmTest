package vn.commentary.backend.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import vn.commentary.backend.admin.security.AdminAuthInterceptor;
import vn.commentary.backend.admin.service.AdminAuthService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Đăng nhập / kiểm tra phiên của quản trị viên. Chỉ {@code /login} là công khai; còn lại do interceptor bảo vệ. */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminAuthService auth;

    public AdminAuthController(AdminAuthService auth) {
        this.auth = auth;
    }

    public record LoginRequest(String email, String password) {
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody(required = false) LoginRequest body, HttpServletRequest request) {
        String email = body == null ? null : body.email();
        String password = body == null ? null : body.password();
        AdminAuthService.LoginResult r = auth.login(email, password, request.getRemoteAddr());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("token", r.token());
        out.put("email", r.email());
        out.put("expiresAt", r.expiresAtEpochSec() * 1000);
        return out;
    }

    /** Kiểm tra token còn dùng được (giao diện gọi khi mở lại trang). */
    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("email", request.getAttribute(AdminAuthInterceptor.ATTR_EMAIL));
        return out;
    }
}
