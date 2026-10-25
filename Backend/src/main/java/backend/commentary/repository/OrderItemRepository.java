package backend.commentary.repository;

import backend.commentary.entity.OrderItem;
import backend.commentary.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Các truy vấn thống kê số lượng đã bán (loại trừ đơn bị HỦY)
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Tổng số lượng đã bán toàn hệ thống
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.order.status <> :cancelled")
    Long sumSoldQuantity(@Param("cancelled") OrderStatus cancelled);

    /**
     * Tổng doanh thu toàn hệ thống (= SUM số lượng * đơn giá)
     */
    @Query("SELECT COALESCE(SUM(oi.quantity * oi.unitPrice), 0) FROM OrderItem oi WHERE oi.order.status <> :cancelled")
    Double sumRevenue(@Param("cancelled") OrderStatus cancelled);

    /**
     * Số lượng đã bán nhóm theo ngày (theo ngày tạo đơn) - để vẽ biểu đồ
     * Trả về: [ngày, số lượng]
     */
    @Query("SELECT CAST(o.createdAt AS date), COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "JOIN oi.order o WHERE o.createdAt >= :startDate AND o.status <> :cancelled " +
           "GROUP BY CAST(o.createdAt AS date) ORDER BY CAST(o.createdAt AS date)")
    List<Object[]> sumSoldQuantityGroupByDate(@Param("startDate") LocalDateTime startDate,
                                              @Param("cancelled") OrderStatus cancelled);

    /**
     * Doanh thu nhóm theo ngày
     * Trả về: [ngày, doanh thu]
     */
    @Query("SELECT CAST(o.createdAt AS date), COALESCE(SUM(oi.quantity * oi.unitPrice), 0) FROM OrderItem oi " +
           "JOIN oi.order o WHERE o.createdAt >= :startDate AND o.status <> :cancelled " +
           "GROUP BY CAST(o.createdAt AS date) ORDER BY CAST(o.createdAt AS date)")
    List<Object[]> sumRevenueGroupByDate(@Param("startDate") LocalDateTime startDate,
                                         @Param("cancelled") OrderStatus cancelled);

    /**
     * Top sản phẩm bán chạy nhất
     * Trả về: [tên sản phẩm, số lượng đã bán, doanh thu]
     */
    @Query("SELECT oi.product.name, COALESCE(SUM(oi.quantity), 0), COALESCE(SUM(oi.quantity * oi.unitPrice), 0) " +
           "FROM OrderItem oi WHERE oi.order.status <> :cancelled " +
           "GROUP BY oi.product.id, oi.product.name ORDER BY COALESCE(SUM(oi.quantity), 0) DESC")
    List<Object[]> sumSoldQuantityGroupByProduct(@Param("cancelled") OrderStatus cancelled, Pageable pageable);
}
