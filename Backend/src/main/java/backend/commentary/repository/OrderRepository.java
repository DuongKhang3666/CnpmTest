package backend.commentary.repository;

import backend.commentary.entity.CustomerOrder;
import backend.commentary.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Query("SELECT o FROM CustomerOrder o WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :query, '%')) OR o.customerPhone LIKE CONCAT('%', :query, '%')) AND " +
           "(:status IS NULL OR o.status = :status)")
    Page<CustomerOrder> searchOrders(@Param("query") String query,
                                     @Param("status") OrderStatus status,
                                     Pageable pageable);

    long countByStatus(OrderStatus status);
}
