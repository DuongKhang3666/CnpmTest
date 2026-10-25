package backend.commentary.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Nhật ký thao tác quản trị")
public class AuditLogResponse {

    @Schema(description = "ID log")
    private Long id;

    @Schema(description = "Email admin thực hiện")
    private String adminEmail;

    @Schema(description = "Hành động: CREATE, UPDATE, DELETE, STATUS_CHANGE, LOGIN")
    private String action;

    @Schema(description = "Loại đối tượng: PLACE, ARTIFACT, ADMIN_USER")
    private String entityType;

    @Schema(description = "ID đối tượng")
    private Long entityId;

    @Schema(description = "Tên đối tượng")
    private String entityName;

    @Schema(description = "Chi tiết thay đổi (JSON)")
    private Map<String, Object> details;

    @Schema(description = "IP client")
    private String ipAddress;

    @Schema(description = "User Agent")
    private String userAgent;

    @Schema(description = "Thời gian thực hiện")
    private LocalDateTime createdAt;

    public AuditLogResponse() {}

    public static AuditLogResponse fromEntity(backend.commentary.entity.AuditLog log) {
        if (log == null) return null;
        AuditLogResponse resp = new AuditLogResponse();
        resp.setId(log.getId());
        resp.setAdminEmail(log.getAdminEmail());
        resp.setAction(log.getAction());
        resp.setEntityType(log.getEntityType());
        resp.setEntityId(log.getEntityId());
        resp.setEntityName(log.getEntityName());
        resp.setIpAddress(log.getIpAddress());
        resp.setUserAgent(log.getUserAgent());
        resp.setCreatedAt(log.getCreatedAt());

        // Parse details JSON
        if (log.getDetails() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                resp.setDetails(mapper.readValue(log.getDetails(), Map.class));
            } catch (Exception e) {
                resp.setDetails(Map.of("raw", log.getDetails()));
            }
        }
        return resp;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}