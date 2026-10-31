package vn.commentary.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import vn.commentary.backend.user.model.Models;

import java.util.List;

/** Các DTO vào/ra của API user. Mỗi record là một hợp đồng JSON. */
public final class Dtos {
    private Dtos() {
    }

    /* ---------- Phiên thiết bị ---------- */
    public record DeviceInfo(String userAgent, String platform, String language, String screen,
                             String timezone, String deviceName, String clientType) {
    }

    public record JoinRequest(String token, String deviceId, String sessionToken, DeviceInfo info) {
    }

    public record JoinResponse(boolean ok, String deviceId, String sessionToken, String expiresAt,
                               int accessHours) {
    }

    public record SessionStatus(boolean ok, String deviceId, String clientType, String expiresAt,
                                long remainingSeconds) {
    }

    /* ---------- Nội dung công khai ---------- */
    public record LanguageDto(long id, String name, String code, String ttsKey) {
    }

    public record SiteSummary(long id, String name, String classify, String city, String address,
                              String description, Double latitude, Double longitude, String imageUrl,
                              int artifactCount) {
    }

    public record ArtifactSummary(long id, long siteId, String siteName, String name, String address,
                                  String description, Double latitude, Double longitude,
                                  String imageUrl) {
    }

    public record SiteDetail(long id, String name, String classify, String city, String address,
                             String description, Double latitude, Double longitude, String imageUrl,
                             List<String> extraImageUrls, List<ArtifactSummary> artifacts) {
    }

    /* ---------- Thuyết minh ---------- */
    public record NarrationDto(String targetType, long targetId, String name, String requestedLanguage,
                               String sourceLanguage, String text, boolean translated,
                               String translationError, boolean ttsEnabled, String ttsKey, double speed,
                               boolean audioEnabled, String audioUrl) {
    }

    public record TranslateRequest(@NotNull @Size(min = 1, max = 50) List<String> texts,
                                   @NotBlank String target, String source) {
    }

    public record TranslatedText(String source, String text, boolean translated, String error) {
    }

    public record TranslateResponse(String target, List<TranslatedText> results) {
    }

    /* ---------- Gói dữ liệu khởi tạo cho trang hành khách ---------- */
    public record CatalogNarration(String targetType, long targetId, String text, boolean useDescription,
                                   boolean ttsEnabled, double speed, boolean audioEnabled, String audioUrl,
                                   Models.GeoConfig gps, Models.BeaconConfig bluetooth, Models.QrConfig qr) {
    }

    public record ListenTotal(String targetType, long targetId, long count) {
    }

    public record Catalog(List<LanguageDto> languages, List<SiteSummary> sites, List<ArtifactSummary> artifacts,
                          List<CatalogNarration> narrations, List<ListenTotal> listens,
                          List<HistoryItem> history, List<LikeItem> likes) {
    }

    /* ---------- Nhận diện (QR / GPS / Beacon) ---------- */
    public record ResolveResult(String targetType, long targetId, String name, String via) {
    }

    public record NearbyMatch(String targetType, long targetId, String name, double distanceMeters,
                              double radiusMeters) {
    }

    /* ---------- Hoạt động người dùng ---------- */
    public record ListenRequest(@NotBlank String targetType, @NotNull Long targetId, String language) {
    }

    public record HistoryRequest(@NotBlank String targetType, @NotNull Long targetId, String language,
                                 String method) {
    }

    public record HistoryItem(long id, String targetType, long targetId, String name, String language,
                              String method, String playedAt) {
    }

    public record LikeRequest(@NotBlank String targetType, @NotNull Long targetId) {
    }

    public record LikeItem(String targetType, long targetId, String name, String createdAt) {
    }

    /* ---------- Quản trị truy cập thiết bị (admin) ---------- */
    public record DeviceDto(String deviceId, String name, String browser, String clientType, String ip,
                            String platform, String language, String screen, String timezone,
                            String firstSeenAt, String lastSeenAt, String expiresAt, int accessHours,
                            boolean online, boolean expired) {
    }

    public record AccessOverview(String qrToken, int accessHours, int maxAppDevices, int activeCount,
                                 int totalCount, int activeAppCount, List<DeviceDto> devices) {
    }

    public record ExtendRequest(Integer hours) {
    }

    public record AccessConfigRequest(Integer maxAppDevices, Integer accessHours) {
    }
}
