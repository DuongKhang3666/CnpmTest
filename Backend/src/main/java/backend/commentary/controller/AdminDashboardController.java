package backend.commentary.controller;

import backend.commentary.dto.response.DashboardStatsResponse;
import backend.commentary.dto.response.SalesStatsResponse;
import backend.commentary.dto.response.UsageStatsResponse;
import backend.commentary.service.DashboardService;
import backend.commentary.service.OrderService;
import backend.commentary.service.VisitorTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Dashboard Admin", description = "APIs thống kê tổng quan cho trang Dashboard quản trị")
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final VisitorTrackingService visitorTrackingService;
    private final OrderService orderService;

    public AdminDashboardController(DashboardService dashboardService,
                                    VisitorTrackingService visitorTrackingService,
                                    OrderService orderService) {
        this.dashboardService = dashboardService;
        this.visitorTrackingService = visitorTrackingService;
        this.orderService = orderService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Lấy thống kê Dashboard", description = "Trả về: tổng số theo loại, theo trạng thái, số chờ duyệt, 10 nội dung mới nhất, số liệu theo ngày (7/30 ngày) để vẽ biểu đồ")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        DashboardStatsResponse stats = dashboardService.getStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/usage")
    @Operation(summary = "Lấy thống kê số người sử dụng trong ngày",
            description = "Trả về: số người sử dụng duy nhất hôm nay, tổng lượt truy cập hôm nay, tổng toàn hệ thống và số người theo ngày (biểu đồ 7/30 ngày)")
    public ResponseEntity<UsageStatsResponse> getUsageStats() {
        UsageStatsResponse usage = visitorTrackingService.getUsageStats();
        return ResponseEntity.ok(usage);
    }

    @GetMapping("/sales")
    @Operation(summary = "Lấy thống kê số lượng đã bán cho khách hàng",
            description = "Trả về: tổng số lượng đã bán, tổng doanh thu, đã bán hôm nay, doanh thu hôm nay, số lượng theo ngày (biểu đồ 7/30 ngày) và top 5 sản phẩm bán chạy (loại trừ đơn bị hủy)")
    public ResponseEntity<SalesStatsResponse> getSalesStats() {
        SalesStatsResponse stats = orderService.getSalesStats();
        return ResponseEntity.ok(stats);
    }
}
