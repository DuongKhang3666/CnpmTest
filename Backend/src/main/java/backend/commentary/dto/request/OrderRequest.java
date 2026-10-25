package backend.commentary.dto.request;

import backend.commentary.entity.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Yêu cầu tạo đơn hàng bán cho khách hàng")
public class OrderRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Schema(description = "Tên khách hàng", example = "Nguyễn Văn A")
    private String customerName;

    @Schema(description = "Số điện thoại khách hàng", example = "0901234567")
    private String customerPhone;

    @Schema(description = "Email khách hàng", example = "khachhang@gmail.com")
    private String customerEmail;

    @Schema(description = "Ghi chú")
    private String note;

    @Schema(description = "Trạng thái đơn hàng (mặc định 'PENDING' - Chờ xử lý)")
    private OrderStatus status = OrderStatus.PENDING;

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    @Valid
    @Schema(description = "Danh sách sản phẩm trong đơn")
    private List<OrderItemRequest> items;

    @Schema(description = "Một dòng sản phẩm trong đơn hàng")
    public static class OrderItemRequest {

        @NotNull(message = "ID sản phẩm không được để trống")
        @Schema(description = "ID sản phẩm")
        private Long productId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
        @Schema(description = "Số lượng bán", example = "2")
        private Long quantity;

        public OrderItemRequest() {
        }

        public OrderItemRequest(Long productId, Long quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Long getQuantity() { return quantity; }
        public void setQuantity(Long quantity) { this.quantity = quantity; }
    }

    public OrderRequest() {
    }

    public OrderRequest(String customerName, String customerPhone, String customerEmail,
                        String note, OrderStatus status, List<OrderItemRequest> items) {
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail;
        this.note = note;
        this.status = status != null ? status : OrderStatus.PENDING;
        this.items = items;
    }

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

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String customerName;
        private String customerPhone;
        private String customerEmail;
        private String note;
        private OrderStatus status = OrderStatus.PENDING;
        private List<OrderItemRequest> items;

        public Builder customerName(String customerName) { this.customerName = customerName; return this; }
        public Builder customerPhone(String customerPhone) { this.customerPhone = customerPhone; return this; }
        public Builder customerEmail(String customerEmail) { this.customerEmail = customerEmail; return this; }
        public Builder note(String note) { this.note = note; return this; }
        public Builder status(OrderStatus status) { this.status = status; return this; }
        public Builder items(List<OrderItemRequest> items) { this.items = items; return this; }

        public OrderRequest build() {
            return new OrderRequest(customerName, customerPhone, customerEmail, note, status, items);
        }
    }
}
