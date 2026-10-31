package vn.commentary.backend.admin.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void wrongHttpMethodIsMethodNotAllowedWithAllowHeader() {
        ResponseEntity<Map<String, Object>> res = handler.unexpected(
                new HttpRequestMethodNotSupportedException("POST", java.util.List.of("GET")));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, res.getStatusCode());
        assertTrue(res.getHeaders().getAllow().contains(HttpMethod.GET));
        assertNotNull(res.getBody().get("error"));
    }

    @Test
    void unknownPathIsNotFound() {
        ResponseEntity<Map<String, Object>> res = handler.unexpected(
                new NoResourceFoundException(HttpMethod.GET, "api/user/khong-co-route"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void responseStatusExceptionKeepsItsStatus() {
        ResponseEntity<Map<String, Object>> res = handler.unexpected(
                new ResponseStatusException(HttpStatus.CONFLICT, "trùng dữ liệu"));
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
    }

    @Test
    void realUnexpectedErrorIsGeneric500AndDoesNotLeakTheMessage() {
        ResponseEntity<Map<String, Object>> res = handler.unexpected(new IllegalStateException("bí mật nội bộ: jdbc://..."));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertEquals("Unexpected server error", res.getBody().get("error"));
        assertFalse(res.getBody().toString().contains("bí mật nội bộ"));
    }

    @Test
    void databaseErrorIsGeneric500AndDoesNotLeakSql() {
        ResponseEntity<Map<String, Object>> res = handler.database(
                new DataAccessResourceFailureException("Unknown column 'secret_col' in 'field list'"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertEquals("Database operation failed", res.getBody().get("error"));
        assertFalse(res.getBody().toString().contains("secret_col"));
    }
}
