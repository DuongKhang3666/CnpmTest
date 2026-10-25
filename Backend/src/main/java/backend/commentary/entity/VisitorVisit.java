package backend.commentary.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity Ghi nhận lượt truy cập của người sử dụng hệ thống (Visitor Visit)
 * Mỗi request tới API được ghi lại 1 dòng để thống kê:
 * - Số lượt truy cập trong ngày (số dòng theo ngày)
 * - Số người sử dụng duy nhất trong ngày (COUNT DISTINCT visitorId)
 * Frontend có thể gửi header "X-Visitor-Id" (UUID cố định theo thiết bị) để nhận diện người dùng,
 * nếu không gửi hệ thống sẽ tự sinh visitorId từ IP + User-Agent.
 */
@Entity
@Table(name = "visitor_visits", indexes = {
        @Index(name = "idx_visit_created", columnList = "createdAt"),
        @Index(name = "idx_visit_visitor", columnList = "visitorId")
})
public class VisitorVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_id", nullable = false, length = 64)
    private String visitorId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "path", length = 500)
    private String path;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public VisitorVisit() {}

    public VisitorVisit(String visitorId, String ipAddress, String userAgent, String path) {
        this.visitorId = visitorId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.path = path;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
