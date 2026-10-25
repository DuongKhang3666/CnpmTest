package backend.commentary.dto.response;

import backend.commentary.entity.CustomerOrder;
import backend.commentary.entity.OrderItem;
import backend.commentary.entity.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Thông tin Đơn hàng bán cho khách hàng")
public class OrderResponse {

    @Schema(description = "ID đơn hàng")
    private Long id;

    @Schema(description = "Mã đơn hàng")
    private String orderCode;

    @Schema(description = "Tên khách hàng")
    private String customerName;

    @Schema(description = "Số điện thoại khách hàng")
    private String customerPhone;

    @Schema(description = "Email khách hàng")
    private String customerEmail;

    @Schema(description = "Ghi chú")
    private String note;

    @Schema(description = "Trạng thái đơn hàng")
    private OrderStatus status;

    @Schema(description = "Tổng tiền đơn hàng")
    private Double totalAmount;

    @Schema(description = "Danh sách sản phẩm trong đơn")
    private List<ItemResponse> items;

    @Schema(description = "Thời gian tạo đơn")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    private LocalDateTime updatedAt;

    public static OrderResponse fromEntity(CustomerOrder order) {
        OrderResponse response = new OrderResponse();
        response.id = order.getId();
        response.orderCode = order.getOrderCode();
        response.customerName = order.getCustomerName();
        response.customerPhone = order.getCustomerPhone();
        response.customerEmail = order.getCustomerEmail();
        response.note = order.getNote();
        response.status = order.getStatus();
        response.totalAmount = order.getTotalAmount();
        response.items = order.getItems().stream()
                .map(ItemResponse::fromEntity)
                .toList();
        response.createdAt = order.getCreatedAt();
        response.updatedAt = order.getUpdatedAt();
        return response;
    }

    @Schema(description = "Một dòng sản phẩm trong đơn hàng")
    public static class ItemResponse {
        @Schema(description = "ID dòng hàng")
        private Long id;

        @Schema(description = "ID sản phẩm")
        private Long productId;

        @Schema(description = "Mã sản phẩm")
        private String productCode;

        @Schema(description = "Tên sản phẩm")
        private String productName;

        @Schema(description = "Số lượng bán")
        private Long quantity;

        @Schema(description = "Đơn giá")
        private Double unitPrice;

        @Schema(description = "Thành tiền")
        private Double subtotal;

        public static ItemResponse fromEntity(OrderItem item) {
            ItemResponse response = new ItemResponse();
            response.id = item.getId();
            response.productId = item.getProduct().getId();
            response.productCode = item.getProduct().getCode();
            response.productName = item.getProduct().getName();
            response.quantity = item.getQuantity();
            response.unitPrice = item.getUnitPrice();
            response.subtotal = item.getSubtotal();
            return response;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Long getQuantity() { return quantity; }
        public void setQuantity(Long quantity) { this.quantity = quantity; }

        public Double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

        public Double getSubtotal() { return subtotal; }
        public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public List<ItemResponse> getItems() { return items; }
    public void setItems(List<ItemResponse> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
