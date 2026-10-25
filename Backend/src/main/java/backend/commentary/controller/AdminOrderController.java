package backend.commentary.controller;

import backend.commentary.dto.request.OrderRequest;
import backend.commentary.dto.request.OrderStatusUpdateRequest;
import backend.commentary.dto.response.OrderResponse;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.entity.enums.OrderStatus;
import backend.commentary.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Quản lý Đơn hàng bán (Admin)", description = "APIs tạo/duyệt/hủy đơn hàng bán sản phẩm cho khách hàng")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách Đơn hàng", description = "Hỗ trợ tìm kiếm theo 'q' (mã đơn, tên hoặc SĐT khách), lọc 'status' và phân trang")
    public ResponseEntity<PageResponse<OrderResponse>> getOrders(
            @Parameter(description = "Từ khóa tìm kiếm (mã đơn / tên khách / SĐT)") @RequestParam(required = false) String q,
            @Parameter(description = "Lọc theo trạng thái ('PENDING', 'PROCESSING', 'COMPLETED', 'CANCELLED')") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Số trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Số mục trên mỗi trang") @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(orderService.getOrders(q, status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết Đơn hàng theo ID")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo Đơn hàng bán cho khách hàng", description = "Trừ tồn kho theo số lượng bán, tự sinh mã đơn DH-yyyyMMddHHmmss-xxxx")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Đổi trạng thái đơn hàng", description = "Hủy đơn (CANCELLED) sẽ hoàn lại tồn kho; đơn hủy chuyển sang trạng thái khác sẽ trừ lại tồn kho")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa Đơn hàng", description = "Xóa đơn và hoàn lại tồn kho nếu đơn chưa bị hủy")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa đơn hàng thành công."));
    }
}
