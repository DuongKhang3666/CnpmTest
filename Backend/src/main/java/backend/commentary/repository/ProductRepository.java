package backend.commentary.repository;

import backend.commentary.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCode(String code);

    @Query("SELECT p FROM Product p WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.code) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:active IS NULL OR p.active = :active)")
    Page<Product> searchProducts(@Param("query") String query,
                                 @Param("active") Boolean active,
                                 Pageable pageable);
}
