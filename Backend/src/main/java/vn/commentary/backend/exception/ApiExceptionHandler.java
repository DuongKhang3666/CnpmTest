package vn.commentary.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.commentary.backend.security.AdminAuthException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> database(DataAccessException ex) {
        // Chi tiết lỗi SQL chỉ ghi vào log, không trả cho client (tránh lộ tên bảng/cột).
        log.error("Lỗi cơ sở dữ liệu", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Database operation failed"));
    }
    @ExceptionHandler(AdminAuthException.class)
    public ResponseEntity<Map<String, Object>> adminAuth(AdminAuthException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("reason", ex.getReason());
        body.put("message", ex.getMessage());
        body.put("error", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Dữ liệu JSON không hợp lệ."));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse spring) {
            // lỗi do client gửi sai (404 không có đường dẫn, 405 sai phương thức, 415...): giữ mã của Spring, không đổi thành 500
            String detail = spring.getBody().getDetail();
            return ResponseEntity.status(spring.getStatusCode()).headers(spring.getHeaders())
                    .body(Map.of("error", detail == null ? "Yêu cầu không hợp lệ." : detail));
        }
        log.error("Lỗi không xử lý được", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Unexpected server error"));
    }
}
