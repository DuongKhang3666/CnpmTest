package backend.commentary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Thống kê số lượng đã bán cho khách hàng")
public class SalesStatsResponse {

    @Schema(description = "Tổng số lượng đã bán toàn hệ thống (không tính đơn bị hủy)")
    private long totalSoldQuantity;

    @Schema(description = "Tổng doanh thu toàn hệ thống")
    private double totalRevenue;

    @Schema(description = "Số lượng đã bán trong hôm nay")
    private long soldToday;

    @Schema(description = "Doanh thu trong hôm nay")
    private double revenueToday;

    @Schema(description = "Số lượng đã bán theo ngày trong 7 ngày gần nhất (key: yyyy-MM-dd)")
    private Map<String, Long> dailySoldLast7Days;

    @Schema(description = "Số lượng đã bán theo ngày trong 30 ngày gần nhất (key: yyyy-MM-dd)")
    private Map<String, Long> dailySoldLast30Days;

    @Schema(description = "Danh sách sản phẩm bán chạy nhất (top 5)")
    private List<TopProduct> topProducts;

    @Schema(description = "Một sản phẩm bán chạy")
    public static class TopProduct {
        @Schema(description = "Tên sản phẩm")
        private String productName;

        @Schema(description = "Số lượng đã bán")
        private long soldQuantity;

        @Schema(description = "Doanh thu")
        private double revenue;

        public TopProduct() {
        }

        public TopProduct(String productName, long soldQuantity, double revenue) {
            this.productName = productName;
            this.soldQuantity = soldQuantity;
            this.revenue = revenue;
        }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public long getSoldQuantity() { return soldQuantity; }
        public void setSoldQuantity(long soldQuantity) { this.soldQuantity = soldQuantity; }

        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
    }

    public SalesStatsResponse() {
    }

    public SalesStatsResponse(long totalSoldQuantity, double totalRevenue,
                              long soldToday, double revenueToday,
                              Map<String, Long> dailySoldLast7Days,
                              Map<String, Long> dailySoldLast30Days,
                              List<TopProduct> topProducts) {
        this.totalSoldQuantity = totalSoldQuantity;
        this.totalRevenue = totalRevenue;
        this.soldToday = soldToday;
        this.revenueToday = revenueToday;
        this.dailySoldLast7Days = dailySoldLast7Days;
        this.dailySoldLast30Days = dailySoldLast30Days;
        this.topProducts = topProducts;
    }

    public long getTotalSoldQuantity() { return totalSoldQuantity; }
    public void setTotalSoldQuantity(long totalSoldQuantity) { this.totalSoldQuantity = totalSoldQuantity; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public long getSoldToday() { return soldToday; }
    public void setSoldToday(long soldToday) { this.soldToday = soldToday; }

    public double getRevenueToday() { return revenueToday; }
    public void setRevenueToday(double revenueToday) { this.revenueToday = revenueToday; }

    public Map<String, Long> getDailySoldLast7Days() { return dailySoldLast7Days; }
    public void setDailySoldLast7Days(Map<String, Long> dailySoldLast7Days) { this.dailySoldLast7Days = dailySoldLast7Days; }

    public Map<String, Long> getDailySoldLast30Days() { return dailySoldLast30Days; }
    public void setDailySoldLast30Days(Map<String, Long> dailySoldLast30Days) { this.dailySoldLast30Days = dailySoldLast30Days; }

    public List<TopProduct> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProduct> topProducts) { this.topProducts = topProducts; }
}
