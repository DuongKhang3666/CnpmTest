package vn.commentary.backend.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.commentary.backend.user.dto.Dtos.HistoryItem;
import vn.commentary.backend.user.dto.Dtos.HistoryRequest;
import vn.commentary.backend.user.dto.Dtos.LikeItem;
import vn.commentary.backend.user.dto.Dtos.LikeRequest;
import vn.commentary.backend.user.dto.Dtos.ListenRequest;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.security.SessionInterceptor;
import vn.commentary.backend.user.service.ActivityService;

import java.util.List;
import java.util.Map;

/** Lượt nghe, lịch sử nghe và yêu thích theo từng thiết bị. */
@RestController
@RequestMapping("/api/user")
public class UserActivityController {
    private final ActivityService activity;

    public UserActivityController(ActivityService activity) {
        this.activity = activity;
    }

    @PostMapping("/listens")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> listen(@Valid @RequestBody ListenRequest request) {
        activity.recordListen(request);
        return Map.of("ok", true);
    }

    @GetMapping("/history")
    public List<HistoryItem> history(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session,
                                     @RequestParam(required = false) Integer limit) {
        return activity.history(session, limit);
    }

    @PostMapping("/history")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addHistory(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session,
                                          @Valid @RequestBody HistoryRequest request) {
        activity.addHistory(session, request);
        return Map.of("ok", true);
    }

    @DeleteMapping("/history")
    public Map<String, Object> clearHistory(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session) {
        return Map.of("ok", true, "deleted", activity.clearHistory(session));
    }

    @GetMapping("/likes")
    public List<LikeItem> likes(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session) {
        return activity.likes(session);
    }

    @PostMapping("/likes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> like(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session,
                                    @Valid @RequestBody LikeRequest request) {
        activity.like(session, request);
        return Map.of("ok", true);
    }

    @DeleteMapping("/likes/{type}/{id}")
    public Map<String, Object> unlike(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session,
                                      @PathVariable String type, @PathVariable long id) {
        activity.unlike(session, type, id);
        return Map.of("ok", true);
    }
}
