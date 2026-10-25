package backend.commentary.service;

import backend.commentary.dto.response.UsageStatsResponse;
import backend.commentary.entity.VisitorVisit;
import backend.commentary.repository.VisitorVisitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service ghi nhận và thống kê số người sử dụng hệ thống theo ngày
 */
@Service
public class VisitorTrackingService {

    public static final String VISITOR_ID_HEADER = "X-Visitor-Id";

    private final VisitorVisitRepository visitorVisitRepository;

    public VisitorTrackingService(VisitorVisitRepository visitorVisitRepository) {
        this.visitorVisitRepository = visitorVisitRepository;
    }

    /**
     * Ghi nhận 1 lượt truy cập (được gọi từ VisitorTrackingFilter cho mọi request API)
     * Không bao giờ làm fail request chính vì lỗi ghi nhận
     */
    @Transactional
    public void recordVisit(HttpServletRequest request) {
        try {
            String visitorId = resolveVisitorId(request);
            String ua = request.getHeader("User-Agent");
            VisitorVisit visit = new VisitorVisit(
                    visitorId,
                    getClientIp(request),
                    ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua,
                    request.getRequestURI())
            ;
            visitorVisitRepository.save(visit);
        } catch (Exception e) {
            System.err.println("Visitor tracking error: " + e.getMessage());
        }
    }

    /**
     * Thống kê số người sử dụng: hôm nay + biểu đồ theo ngày (7/30 ngày gần nhất)
     */
    @Transactional(readOnly = true)
    public UsageStatsResponse getUsageStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long todayVisits = visitorVisitRepository.countByCreatedAtBetween(todayStart, now);
        Long todayUniqueUsers = visitorVisitRepository.countDistinctVisitorsBetween(todayStart, now);

        long totalVisits = visitorVisitRepository.count();
        Long totalUniqueUsers = visitorVisitRepository.countDistinctVisitors();

        Map<String, Long> dailyUniqueLast7Days = getDailyUniqueStats(7);
        Map<String, Long> dailyUniqueLast30Days = getDailyUniqueStats(30);

        return new UsageStatsResponse(
                todayUniqueUsers != null ? todayUniqueUsers : 0L,
                todayVisits,
                totalUniqueUsers != null ? totalUniqueUsers : 0L,
                totalVisits,
                dailyUniqueLast7Days,
                dailyUniqueLast30Days
        );
    }

    /**
     * Số người sử dụng duy nhất theo ngày trong N ngày gần nhất (1 truy vấn GROUP BY)
     */
    private Map<String, Long> getDailyUniqueStats(int days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        List<Object[]> rows = visitorVisitRepository.countGroupByDate(startDateTime);

        Map<String, Long> stats = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            stats.put(date.toString(), 0L);
        }

        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            Long uniqueCount = (Long) row[1];
            if (date != null && uniqueCount != null && stats.containsKey(date.toString())) {
                stats.merge(date.toString(), uniqueCount, Long::sum);
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

    /**
     * Xác định ID người dùng: ưu tiên header "X-Visitor-Id", nếu không có thì hash từ IP + User-Agent
     */
    private String resolveVisitorId(HttpServletRequest request) {
        String visitorId = request.getHeader(VISITOR_ID_HEADER);
        if (visitorId == null || visitorId.isBlank()) {
            String fingerprint = getClientIp(request) + "|" + request.getHeader("User-Agent");
            visitorId = Integer.toHexString(fingerprint.hashCode());
        }
        return visitorId.length() > 64 ? visitorId.substring(0, 64) : visitorId;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }
}
