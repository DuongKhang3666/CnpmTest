package backend.commentary.repository;

import backend.commentary.entity.Artifact;
import backend.commentary.entity.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    Optional<Artifact> findByCode(String code);

    boolean existsByCode(String code);

    long countByStatus(ContentStatus status);

    @Query("SELECT a FROM Artifact a WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.code) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:placeId IS NULL OR (a.place IS NOT NULL AND a.place.id = :placeId)) AND " +
           "(:status IS NULL OR a.status = :status)")
    Page<Artifact> searchArtifacts(@Param("query") String query,
                                   @Param("placeId") Long placeId,
                                   @Param("status") ContentStatus status,
                                   Pageable pageable);

    List<Artifact> findTop5ByOrderByCreatedAtDesc();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT CAST(a.createdAt AS date), COUNT(a) FROM Artifact a WHERE a.createdAt >= :startDate GROUP BY CAST(a.createdAt AS date)")
    List<Object[]> countByCreatedAtGroupByDate(@Param("startDate") LocalDateTime startDate);
}
