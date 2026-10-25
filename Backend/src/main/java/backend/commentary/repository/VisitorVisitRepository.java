package backend.commentary.repository;

import backend.commentary.entity.VisitorVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitorVisitRepository extends JpaRepository<VisitorVisit, Long> {

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT v.visitorId) FROM VisitorVisit v WHERE v.createdAt BETWEEN :start AND :end")
    Long countDistinctVisitorsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT v.visitorId) FROM VisitorVisit v")
    Long countDistinctVisitors();

    /**
     * Nhóm theo ngày: ngày, số người duy nhất, tổng lượt truy cập
     */
    @Query("SELECT CAST(v.createdAt AS date), COUNT(DISTINCT v.visitorId), COUNT(v) FROM VisitorVisit v " +
           "WHERE v.createdAt >= :startDate GROUP BY CAST(v.createdAt AS date) ORDER BY CAST(v.createdAt AS date)")
    List<Object[]> countGroupByDate(@Param("startDate") LocalDateTime startDate);
}
