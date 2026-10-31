package vn.commentary.backend.user.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.commentary.backend.exception.ResourceNotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Xử lý lỗi riêng cho API user. Chạy trước {@code ApiExceptionHandler} (admin) nhưng chỉ áp dụng cho
 * controller trong package {@code vn.commentary.backend.user}, nên không đổi hành vi của API admin cũ.
 */
@RestControllerAdvice(basePackages = "vn.commentary.backend.user")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(UserExceptionHandler.class);

    @ExceptionHandler(UserAccessException.class)
    public ResponseEntity<Map<String, Object>> access(UserAccessException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("reason", ex.getReason());
        body.put("message", ex.getMessage());
        body.put("error", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException ex) {
        FieldError fe = ex.getBindingResult().getFieldError();
        String msg = fe == null ? "Dữ liệu không hợp lệ." : fe.getField() + ": " + fe.getDefaultMessage();
        return error(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<Map<String, Object>> malformed(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ hoặc thiếu tham số.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> database(DataAccessException ex) {
        log.error("Lỗi truy cập cơ sở dữ liệu (user API)", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống dữ liệu, vui lòng thử lại sau.");
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Map<String, Object>> json(JsonProcessingException ex) {
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu JSON không hợp lệ.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
