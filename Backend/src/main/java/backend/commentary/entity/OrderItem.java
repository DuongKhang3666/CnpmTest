package backend.commentary.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity Chi tiết đơn hàng (mỗi dòng = 1 sản phẩm + số lượng bán)
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_item_product", columnList = "productId")
})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orderId", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "productId", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Long quantity; // số lượng đã bán của sản phẩm này

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice; // giá bán tại thời điểm đặt đơn

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OrderItem() {}

    public OrderItem(Product product, Long quantity, Double unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public double getSubtotal() {
        return quantity != null && unitPrice != null ? quantity * unitPrice : 0d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
