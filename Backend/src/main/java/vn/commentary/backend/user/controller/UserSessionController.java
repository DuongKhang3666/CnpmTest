package vn.commentary.backend.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.commentary.backend.user.dto.Dtos.JoinRequest;
import vn.commentary.backend.user.dto.Dtos.JoinResponse;
import vn.commentary.backend.user.dto.Dtos.SessionStatus;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.security.SessionInterceptor;
import vn.commentary.backend.user.service.SessionService;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/** Vào hệ thống bằng QR, giữ phiên (heartbeat) và rời hệ thống. */
@RestController
@RequestMapping("/api/user")
public class UserSessionController {
    private final SessionService sessionService;
    private final Clock clock;

    public UserSessionController(SessionService sessionService, Clock clock) {
        this.sessionService = sessionService;
        this.clock = clock;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("service", "commentary-user-api");
        body.put("time", clock.instant().toString());
        return body;
    }

    @PostMapping("/session/join")
    public JoinResponse join(@RequestBody(required = false) JoinRequest request, HttpServletRequest http) {
        return sessionService.join(request, clientIp(http));
    }

    @PostMapping("/session/heartbeat")
    public SessionStatus heartbeat(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session) {
        return sessionService.status(session);
    }

    @GetMapping("/session/me")
    public SessionStatus me(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session) {
        return sessionService.status(session);
    }

    @PostMapping("/session/leave")
    public Map<String, Object> leave(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session) {
        sessionService.leave(session);
        return Map.of("ok", true);
    }

    private static String clientIp(HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        return ip == null ? "" : ip.replaceFirst("^::ffff:", "");
    }
}
