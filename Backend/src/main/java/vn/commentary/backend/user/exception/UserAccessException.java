package vn.commentary.backend.user.exception;

import org.springframework.http.HttpStatus;

/** Lỗi truy cập của người dùng (chưa/không còn quyền, hết hạn, quá giới hạn thiết bị...). */
public class UserAccessException extends RuntimeException {
    private final HttpStatus status;
    private final String reason;

    public UserAccessException(HttpStatus status, String reason, String message) {
        super(message);
        this.status = status;
        this.reason = reason;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Mã lý do ổn định để client xử lý: no-session, invalid-session, expired, no-token, token-invalid, app-limit, bad-device... */
    public String getReason() {
        return reason;
    }
}
