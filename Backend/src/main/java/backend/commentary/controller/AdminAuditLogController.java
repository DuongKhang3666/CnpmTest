package backend.commentary.controller;

import backend.commentary.dto.response.AuditLogResponse;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "Nhật ký thao tác (Audit Log)", description = "APIs tra cứu lịch sử thao tác của admin trên hệ thống")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Tra cứu nhật ký thao tác", description = "Lọc theo admin, loại đối tượng, hành động, khoảng thời gian. Phân trang.")
    public ResponseEntity<PageResponse<AuditLogResponse>> searchLogs(
            @Parameter(description = "Email admin") @RequestParam(required = false) String adminEmail,
            @Parameter(description = "Loại đối tượng: PLACE, ARTIFACT, ADMIN_USER") @RequestParam(required = false) String entityType,
            @Parameter(description = "Hành động: CREATE, UPDATE, DELETE, STATUS_CHANGE, LOGIN") @RequestParam(required = false) String action,
            @Parameter(description = "Từ ngày (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "Đến ngày (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Parameter(description = "Trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Số mục/trang (max 100)") @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<AuditLogResponse> response = auditLogService.searchLogs(adminEmail, entityType, action, fromDate, toDate, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @Operation(summary = "20 thao tác gần nhất", description = "Dành cho widget Dashboard hiển thị hoạt động gần đây")
    public ResponseEntity<List<AuditLogResponse>> getRecentLogs() {
        return ResponseEntity.ok(auditLogService.getRecentLogs());
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Lịch sử của 1 đối tượng cụ thể", description = "Xem toàn bộ thao tác liên quan đến 1 Place/Artifact/AdminUser")
    public ResponseEntity<List<AuditLogResponse>> getEntityHistory(
            @Parameter(description = "Loại: PLACE, ARTIFACT, ADMIN_USER") @PathVariable String entityType,
            @Parameter(description = "ID đối tượng") @PathVariable Long entityId
    ) {
        return ResponseEntity.ok(auditLogService.getEntityHistory(entityType, entityId));
    }
}