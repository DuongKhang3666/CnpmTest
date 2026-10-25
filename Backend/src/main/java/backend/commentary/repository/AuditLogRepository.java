package backend.commentary.repository;

import backend.commentary.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByAdminEmail(String adminEmail, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:adminEmail IS NULL OR a.adminEmail = :adminEmail) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:fromDate IS NULL OR a.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR a.createdAt <= :toDate)")
    Page<AuditLog> searchLogs(
            @Param("adminEmail") String adminEmail,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    List<AuditLog> findTop20ByOrderByCreatedAtDesc();
}