package backend.commentary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Thống kê số người sử dụng hệ thống")
public class UsageStatsResponse {

    @Schema(description = "Số người sử dụng duy nhất trong hôm nay")
    private long todayUniqueUsers;

    @Schema(description = "Tổng số lượt truy cập trong hôm nay")
    private long todayVisits;

    @Schema(description = "Tổng số người sử dụng duy nhất từ trước đến nay")
    private long totalUniqueUsers;

    @Schema(description = "Tổng số lượt truy cập từ trước đến nay")
    private long totalVisits;

    @Schema(description = "Số người sử dụng duy nhất theo ngày trong 7 ngày gần nhất (key: yyyy-MM-dd)")
    private Map<String, Long> dailyUniqueLast7Days;

    @Schema(description = "Số người sử dụng duy nhất theo ngày trong 30 ngày gần nhất (key: yyyy-MM-dd)")
    private Map<String, Long> dailyUniqueLast30Days;

    public UsageStatsResponse() {
    }

    public UsageStatsResponse(long todayUniqueUsers, long todayVisits,
                              long totalUniqueUsers, long totalVisits,
                              Map<String, Long> dailyUniqueLast7Days,
                              Map<String, Long> dailyUniqueLast30Days) {
        this.todayUniqueUsers = todayUniqueUsers;
        this.todayVisits = todayVisits;
        this.totalUniqueUsers = totalUniqueUsers;
        this.totalVisits = totalVisits;
        this.dailyUniqueLast7Days = dailyUniqueLast7Days;
        this.dailyUniqueLast30Days = dailyUniqueLast30Days;
    }

    public long getTodayUniqueUsers() { return todayUniqueUsers; }
    public void setTodayUniqueUsers(long todayUniqueUsers) { this.todayUniqueUsers = todayUniqueUsers; }

    public long getTodayVisits() { return todayVisits; }
    public void setTodayVisits(long todayVisits) { this.todayVisits = todayVisits; }

    public long getTotalUniqueUsers() { return totalUniqueUsers; }
    public void setTotalUniqueUsers(long totalUniqueUsers) { this.totalUniqueUsers = totalUniqueUsers; }

    public long getTotalVisits() { return totalVisits; }
    public void setTotalVisits(long totalVisits) { this.totalVisits = totalVisits; }

    public Map<String, Long> getDailyUniqueLast7Days() { return dailyUniqueLast7Days; }
    public void setDailyUniqueLast7Days(Map<String, Long> dailyUniqueLast7Days) { this.dailyUniqueLast7Days = dailyUniqueLast7Days; }

    public Map<String, Long> getDailyUniqueLast30Days() { return dailyUniqueLast30Days; }
    public void setDailyUniqueLast30Days(Map<String, Long> dailyUniqueLast30Days) { this.dailyUniqueLast30Days = dailyUniqueLast30Days; }
}
