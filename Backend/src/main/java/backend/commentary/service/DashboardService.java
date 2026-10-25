package backend.commentary.service;

import backend.commentary.dto.response.DashboardStatsResponse;
import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.entity.enums.PlaceType;
import backend.commentary.repository.PlaceRepository;
import backend.commentary.repository.ArtifactRepository;
import backend.commentary.repository.AdminUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service cung cấp số liệu thống kê cho Dashboard Admin
 */
@Service
public class DashboardService {

    private final PlaceRepository placeRepository;
    private final ArtifactRepository artifactRepository;
    private final AdminUserRepository adminUserRepository;

    public DashboardService(PlaceRepository placeRepository,
                            ArtifactRepository artifactRepository,
                            AdminUserRepository adminUserRepository) {
        this.placeRepository = placeRepository;
        this.artifactRepository = artifactRepository;
        this.adminUserRepository = adminUserRepository;
    }

    /**
     * Lấy toàn bộ số liệu dashboard
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        // 1. Tổng số theo loại
        long totalPlaces = placeRepository.count();
        long totalArtifacts = artifactRepository.count();
        long totalAdmins = adminUserRepository.count();

        // Phân chia Place theo type
        long totalDiaDiem = placeRepository.countByType(PlaceType.DIA_DIEM);
        long totalBaoTang = placeRepository.countByType(PlaceType.BAO_TANG);

        // 2. Tổng số theo trạng thái (gộp Place + Artifact)
        long publishedCount = placeRepository.countByStatus(ContentStatus.PUBLISHED)
                + artifactRepository.countByStatus(ContentStatus.PUBLISHED);
        long pendingCount = placeRepository.countByStatus(ContentStatus.PENDING)
                + artifactRepository.countByStatus(ContentStatus.PENDING);
        long draftCount = placeRepository.countByStatus(ContentStatus.DRAFT)
                + artifactRepository.countByStatus(ContentStatus.DRAFT);

        // 3. Số mục chờ duyệt (Place + Artifact)
        long pendingApproval = pendingCount;

        // 4. Nội dung mới thêm gần đây (5 Place + 5 Artifact mới nhất)
        List<DashboardStatsResponse.RecentItem> recentItems = new ArrayList<>();
        List<DashboardStatsResponse.RecentItem> tempItems = new ArrayList<>();
        placeRepository.findTop5ByOrderByCreatedAtDesc().forEach(p ->
                tempItems.add(new DashboardStatsResponse.RecentItem(
                        p.getId(), "Địa điểm/Bảo tàng", p.getName(), p.getStatus(), p.getCreatedAt()
                ))
        );
        artifactRepository.findTop5ByOrderByCreatedAtDesc().forEach(a ->
                tempItems.add(new DashboardStatsResponse.RecentItem(
                        a.getId(), "Hiện vật", a.getName(), a.getStatus(), a.getCreatedAt()
                ))
        );
        // Sắp xếp theo createdAt giảm dần, lấy top 10
        tempItems.sort((x, y) -> y.getCreatedAt().compareTo(x.getCreatedAt()));
        if (tempItems.size() > 10) {
            recentItems = tempItems.subList(0, 10);
        } else {
            recentItems = tempItems;
        }

        // 5. Số liệu theo thời gian (7 ngày gần nhất và 30 ngày gần nhất)
        Map<String, Long> dailyCreatedLast7Days = getDailyCreatedStats(7);
        Map<String, Long> dailyCreatedLast30Days = getDailyCreatedStats(30);

        // 6. Phân bố trạng thái chi tiết cho biểu đồ
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        statusDistribution.put("Đã xuất bản", publishedCount);
        statusDistribution.put("Chờ duyệt", pendingCount);
        statusDistribution.put("Bản nháp", draftCount);

        return new DashboardStatsResponse(
                totalPlaces,
                totalArtifacts,
                totalAdmins,
                totalDiaDiem,
                totalBaoTang,
                publishedCount,
                pendingCount,
                draftCount,
                pendingApproval,
                recentItems,
                dailyCreatedLast7Days,
                dailyCreatedLast30Days,
                statusDistribution
        );
    }

    /**
     * Lấy số lượng bản ghi tạo mới theo ngày trong N ngày gần nhất (tối ưu: 2 truy vấn GROUP BY)
     */
    private Map<String, Long> getDailyCreatedStats(int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        // 1 truy vấn cho Place, 1 truy vấn cho Artifact (tổng 2 thay vì 2*days)
        List<Object[]> placeStats = placeRepository.countByCreatedAtGroupByDate(startDateTime);
        List<Object[]> artifactStats = artifactRepository.countByCreatedAtGroupByDate(startDateTime);

        // Gộp kết quả vào map
        Map<String, Long> stats = new LinkedHashMap<>();

        // Khởi tạo tất cả các ngày trong khoảng với giá trị 0
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            stats.put(date.toString(), 0L);
        }

        // Cộng dồn từ Place
        for (Object[] row : placeStats) {
            LocalDate date = toLocalDate(row[0]);
            Long count = (Long) row[1];
            if (date != null && count != null) {
                stats.merge(date.toString(), count, Long::sum);
            }
        }

        // Cộng dồn từ Artifact
        for (Object[] row : artifactStats) {
            LocalDate date = toLocalDate(row[0]);
            Long count = (Long) row[1];
            if (date != null && count != null) {
                stats.merge(date.toString(), count, Long::sum);
            }
        }

        return stats;
    }

    /**
     * Chuyển kết quả CAST(x AS date) về LocalDate (H2 trả java.sql.Date, MySQL trả LocalDate)
     */
    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return null;
    }
}