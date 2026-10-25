package backend.commentary.dto.response;

import backend.commentary.entity.enums.ContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Thống kê tổng quan Dashboard Admin")
public class DashboardStatsResponse {

    @Schema(description = "Tổng số Địa điểm & Bảo tàng")
    private long totalPlaces;

    @Schema(description = "Tổng số Hiện vật")
    private long totalArtifacts;

    @Schema(description = "Tổng số tài khoản Admin")
    private long totalAdmins;

    @Schema(description = "Số lượng Địa điểm du lịch")
    private long totalDiaDiem;

    @Schema(description = "Số lượng Bảo tàng")
    private long totalBaoTang;

    @Schema(description = "Tổng số nội dung 'Đã xuất bản' (Place + Artifact)")
    private long publishedCount;

    @Schema(description = "Tổng số nội dung 'Chờ duyệt' (Place + Artifact)")
    private long pendingCount;

    @Schema(description = "Tổng số nội dung 'Bản nháp' (Place + Artifact)")
    private long draftCount;

    @Schema(description = "Số mục chờ duyệt (tổng 'Chờ duyệt')")
    private long pendingApproval;

    @Schema(description = "Danh sách 10 nội dung mới thêm gần đây")
    private List<RecentItem> recentItems;

    @Schema(description = "Số lượng nội dung tạo mới theo ngày (7 ngày gần nhất, key: yyyy-MM-dd)")
    private Map<String, Long> dailyCreatedLast7Days;

    @Schema(description = "Số lượng nội dung tạo mới theo ngày (30 ngày gần nhất, key: yyyy-MM-dd)")
    private Map<String, Long> dailyCreatedLast30Days;

    @Schema(description = "Phân bố trạng thái cho biểu đồ tròn (key: tên trạng thái tiếng Việt, value: số lượng)")
    private Map<String, Long> statusDistribution;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long totalPlaces, long totalArtifacts, long totalAdmins,
                                  long totalDiaDiem, long totalBaoTang,
                                  long publishedCount, long pendingCount, long draftCount,
                                  long pendingApproval, List<RecentItem> recentItems,
                                  Map<String, Long> dailyCreatedLast7Days,
                                  Map<String, Long> dailyCreatedLast30Days,
                                  Map<String, Long> statusDistribution) {
        this.totalPlaces = totalPlaces;
        this.totalArtifacts = totalArtifacts;
        this.totalAdmins = totalAdmins;
        this.totalDiaDiem = totalDiaDiem;
        this.totalBaoTang = totalBaoTang;
        this.publishedCount = publishedCount;
        this.pendingCount = pendingCount;
        this.draftCount = draftCount;
        this.pendingApproval = pendingApproval;
        this.recentItems = recentItems;
        this.dailyCreatedLast7Days = dailyCreatedLast7Days;
        this.dailyCreatedLast30Days = dailyCreatedLast30Days;
        this.statusDistribution = statusDistribution;
    }

    public long getTotalPlaces() { return totalPlaces; }
    public void setTotalPlaces(long totalPlaces) { this.totalPlaces = totalPlaces; }

    public long getTotalArtifacts() { return totalArtifacts; }
    public void setTotalArtifacts(long totalArtifacts) { this.totalArtifacts = totalArtifacts; }

    public long getTotalAdmins() { return totalAdmins; }
    public void setTotalAdmins(long totalAdmins) { this.totalAdmins = totalAdmins; }

    public long getTotalDiaDiem() { return totalDiaDiem; }
    public void setTotalDiaDiem(long totalDiaDiem) { this.totalDiaDiem = totalDiaDiem; }

    public long getTotalBaoTang() { return totalBaoTang; }
    public void setTotalBaoTang(long totalBaoTang) { this.totalBaoTang = totalBaoTang; }

    public long getPublishedCount() { return publishedCount; }
    public void setPublishedCount(long publishedCount) { this.publishedCount = publishedCount; }

    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }

    public long getDraftCount() { return draftCount; }
    public void setDraftCount(long draftCount) { this.draftCount = draftCount; }

    public long getPendingApproval() { return pendingApproval; }
    public void setPendingApproval(long pendingApproval) { this.pendingApproval = pendingApproval; }

    public List<RecentItem> getRecentItems() { return recentItems; }
    public void setRecentItems(List<RecentItem> recentItems) { this.recentItems = recentItems; }

    public Map<String, Long> getDailyCreatedLast7Days() { return dailyCreatedLast7Days; }
    public void setDailyCreatedLast7Days(Map<String, Long> dailyCreatedLast7Days) { this.dailyCreatedLast7Days = dailyCreatedLast7Days; }

    public Map<String, Long> getDailyCreatedLast30Days() { return dailyCreatedLast30Days; }
    public void setDailyCreatedLast30Days(Map<String, Long> dailyCreatedLast30Days) { this.dailyCreatedLast30Days = dailyCreatedLast30Days; }

    public Map<String, Long> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }

    @Schema(description = "Mục nội dung mới thêm gần đây")
    public static class RecentItem {
        @Schema(description = "ID bản ghi")
        private Long id;

        @Schema(description = "Loại nội dung: 'Địa điểm/Bảo tàng' hoặc 'Hiện vật'")
        private String type;

        @Schema(description = "Tên nội dung")
        private String name;

        @Schema(description = "Trạng thái")
        private ContentStatus status;

        @Schema(description = "Thời gian tạo")
        private LocalDateTime createdAt;

        public RecentItem() {}

        public RecentItem(Long id, String type, String name, ContentStatus status, LocalDateTime createdAt) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.status = status;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public ContentStatus getStatus() { return status; }
        public void setStatus(ContentStatus status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}