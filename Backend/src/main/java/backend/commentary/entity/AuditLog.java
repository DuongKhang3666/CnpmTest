package backend.commentary.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Nhật ký thao tác quản trị (Audit Log)
 * Ghi lại: ai, làm gì, đối tượng nào, khi nào, chi tiết thay đổi
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_admin", columnList = "adminEmail"),
        @Index(name = "idx_audit_entity", columnList = "entityType,entityId"),
        @Index(name = "idx_audit_created", columnList = "createdAt")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_email", nullable = false, length = 100)
    private String adminEmail;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE, STATUS_CHANGE, LOGIN

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // PLACE, ARTIFACT, ADMIN_USER

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name", length = 255)
    private String entityName;

    @Column(columnDefinition = "TEXT")
    private String details; // JSON mô tả thay đổi (old/new values)

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(String adminEmail, String action, String entityType, Long entityId,
                    String entityName, String details, String ipAddress, String userAgent) {
        this.adminEmail = adminEmail;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityName = entityName;
        this.details = details;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
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

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String adminEmail;
        private String action;
        private String entityType;
        private Long entityId;
        private String entityName;
        private String details;
        private String ipAddress;
        private String userAgent;

        public Builder adminEmail(String v) { adminEmail = v; return this; }
        public Builder action(String v) { action = v; return this; }
        public Builder entityType(String v) { entityType = v; return this; }
        public Builder entityId(Long v) { entityId = v; return this; }
        public Builder entityName(String v) { entityName = v; return this; }
        public Builder details(String v) { details = v; return this; }
        public Builder ipAddress(String v) { ipAddress = v; return this; }
        public Builder userAgent(String v) { userAgent = v; return this; }

        public AuditLog build() {
            return new AuditLog(adminEmail, action, entityType, entityId, entityName, details, ipAddress, userAgent);
        }
    }
}