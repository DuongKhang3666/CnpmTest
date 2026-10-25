package backend.commentary.dto.request;

import backend.commentary.entity.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu thay đổi trạng thái đơn hàng")
public class OrderStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    @Schema(description = "Trạng thái mới: 'PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED'", example = "COMPLETED")
    private OrderStatus status;

    public OrderStatusUpdateRequest() {
    }

    public OrderStatusUpdateRequest(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
