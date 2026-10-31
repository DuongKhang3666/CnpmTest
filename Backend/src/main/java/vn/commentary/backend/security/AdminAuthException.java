package vn.commentary.backend.security;

import org.springframework.http.HttpStatus;

/** Lỗi xác thực quản trị: sai tài khoản/mật khẩu, thử quá nhiều lần... */
public class AdminAuthException extends RuntimeException {
    private final HttpStatus status;
    private final String reason;

    public AdminAuthException(HttpStatus status, String reason, String message) {
        super(message);
        this.status = status;
        this.reason = reason;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
