package backend.commentary.service;

import backend.commentary.dto.response.AuditLogResponse;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.entity.AuditLog;
import backend.commentary.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service ghi và truy vấn nhật ký thao tác (Audit Log)
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Ghi log thao tác (gọi async trong service khác)
     */
    @Transactional
    public void log(String adminEmail, String action, String entityType, Long entityId,
                    String entityName, Map<String, Object> details,
                    HttpServletRequest request) {
        try {
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;
            String ip = getClientIp(request);
            String ua = request != null ? request.getHeader("User-Agent") : null;

            AuditLog log = AuditLog.builder()
                    .adminEmail(adminEmail)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName)
                    .details(detailsJson)
                    .ipAddress(ip)
                    .userAgent(ua)
                    .build();

            auditLogRepository.save(log);
        } catch (Exception e) {
            // Không bao giờ làm fail request chính vì lỗi ghi log
            System.err.println("Audit log error: " + e.getMessage());
        }
    }

    /**
     * Ghi log đơn giản (không có request context)
     */
    @Transactional
    public void logSimple(String adminEmail, String action, String entityType, Long entityId,
                          String entityName, Map<String, Object> details) {
        log(adminEmail, action, entityType, entityId, entityName, details, null);
    }

    /**
     * Tra cứu log với bộ lọc
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> searchLogs(String adminEmail, String entityType,
                                                      String action, LocalDateTime fromDate,
                                                      LocalDateTime toDate, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AuditLog> logPage = auditLogRepository.searchLogs(adminEmail, entityType, action, fromDate, toDate, pageable);

        List<AuditLogResponse> items = logPage.getContent().stream()
                .map(AuditLogResponse::fromEntity)
                .toList();

        return new PageResponse<>(items, page, pageSize, logPage.getTotalElements(),
                Math.max(1, logPage.getTotalPages()));
    }

    /**
     * 20 log gần nhất cho dashboard
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecentLogs() {
        return auditLogRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }

    /**
     * Log lịch sử của 1 entity cụ thể
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, PageRequest.of(0, 50))
                .stream().map(AuditLogResponse::fromEntity).toList();
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }
}