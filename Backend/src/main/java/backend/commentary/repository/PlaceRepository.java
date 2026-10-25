package backend.commentary.repository;

import backend.commentary.entity.Place;
import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.entity.enums.PlaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByCode(String code);

    boolean existsByCode(String code);

    long countByType(PlaceType type);

    long countByStatus(ContentStatus status);

    @Query("SELECT p FROM Place p WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:type IS NULL OR p.type = :type) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Place> searchPlaces(@Param("query") String query,
                             @Param("type") PlaceType type,
                             @Param("status") ContentStatus status,
                             Pageable pageable);

    List<Place> findTop5ByOrderByCreatedAtDesc();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT CAST(p.createdAt AS date), COUNT(p) FROM Place p WHERE p.createdAt >= :startDate GROUP BY CAST(p.createdAt AS date)")
    List<Object[]> countByCreatedAtGroupByDate(@Param("startDate") LocalDateTime startDate);
}
