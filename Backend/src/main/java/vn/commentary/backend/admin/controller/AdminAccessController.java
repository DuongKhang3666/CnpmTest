package vn.commentary.backend.admin.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.commentary.backend.user.dto.Dtos.AccessConfigRequest;
import vn.commentary.backend.user.dto.Dtos.AccessOverview;
import vn.commentary.backend.user.dto.Dtos.DeviceDto;
import vn.commentary.backend.user.dto.Dtos.ExtendRequest;
import vn.commentary.backend.admin.service.AccessAdminService;

import org.springframework.beans.factory.annotation.Value;
import vn.commentary.backend.common.util.NetworkPolicy;
import vn.commentary.backend.common.util.NetworkPolicy.LanAddress;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API quản trị truy cập của thiết bị người dùng (mã QR, danh sách thiết bị, gia hạn, thu hồi, cấu hình).
 * Được {@code AdminAuthInterceptor} bảo vệ như mọi API quản trị: {@code Authorization: Bearer <token>}
 * hoặc header {@code X-Admin-Key} khi đã cấu hình {@code admin.api-key}.
 */
@RestController
@RequestMapping("/api/admin/access")
public class AdminAccessController {
    private final AccessAdminService service;

    private final String publicHost;

    public AdminAccessController(AccessAdminService service,
                                 @Value("${user.public-host:}") String publicHost) {
        this.service = service;
        this.publicHost = publicHost == null ? "" : publicHost.trim();
    }

    /** Token kèm địa chỉ máy chủ để admin dựng link QR: {@code PUBLIC_HOST} nếu đặt, nếu không thì IP LAN. */
    private Map<String, Object> qrBody(String token) {
        List<LanAddress> ips = publicHost.isEmpty() ? NetworkPolicy.lanAddresses()
                : List.of(new LanAddress("PUBLIC_HOST", publicHost));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("ips", ips);
        return body;
    }

    /** Tổng quan: QR hiện hành, số người đang dùng và danh sách thiết bị. */
    @GetMapping("/overview")
    public AccessOverview overview() {
        return service.overview();
    }

    @GetMapping("/qr")
    public Map<String, Object> qr() {
        return qrBody(service.currentQrToken());
    }

    @PostMapping("/qr/rotate")
    public Map<String, Object> rotate() {
        return qrBody(service.rotateQrToken());
    }

    @PostMapping("/devices/{deviceId}/extend")
    public DeviceDto extend(@PathVariable String deviceId, @RequestBody(required = false) ExtendRequest request) {
        return service.extend(deviceId, request == null ? null : request.hours());
    }

    @DeleteMapping("/devices/{deviceId}")
    public Map<String, Object> revoke(@PathVariable String deviceId) {
        return Map.of("ok", true, "token", service.revoke(deviceId));
    }

    @PostMapping("/devices/clear-offline")
    public Map<String, Object> clearOffline() {
        return Map.of("ok", true, "deleted", service.clearOffline());
    }

    @PutMapping("/config")
    public Map<String, Object> config(@RequestBody AccessConfigRequest request) {
        if (request.maxAppDevices() == null && request.accessHours() == null) {
            throw new IllegalArgumentException("Cần gửi maxAppDevices hoặc accessHours.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        if (request.maxAppDevices() != null) {
            service.setMaxAppDevices(request.maxAppDevices());
            body.put("maxAppDevices", request.maxAppDevices());
        }
        if (request.accessHours() != null) {
            service.setAccessHours(request.accessHours());
            body.put("accessHours", request.accessHours());
        }
        return body;
    }
}
