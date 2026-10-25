package backend.commentary.dto.response;

import backend.commentary.entity.Artifact;
import backend.commentary.entity.enums.ContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Schema(description = "Thông tin chi tiết Hiện vật (Artifact) trả về cho Client")
public class ArtifactResponse {

    @Schema(description = "ID bản ghi")
    private Long id;

    @Schema(description = "Mã định danh tự sinh (HV-xxx)", example = "HV-001")
    private String code;

    @Schema(description = "Tên hiện vật", example = "Xe Tăng 390")
    private String name;

    @Schema(description = "Loại nội dung (luôn là 'Hiện vật')", example = "Hiện vật")
    private String type = "Hiện vật";

    @Schema(description = "Mô tả hiện vật")
    private String description;

    @Schema(description = "Trạng thái", example = "Đã xuất bản")
    private ContentStatus status;

    @Schema(description = "URL ảnh hiện vật")
    private String image;

    @Schema(description = "ID của địa điểm/bảo tàng liên kết (null nếu là hiện vật độc lập)")
    private Long placeId;

    @Schema(description = "Tên địa điểm/bảo tàng liên kết")
    private String placeName;

    @Schema(description = "Vị trí địa điểm hoặc tọa độ hiển thị trên bảng UI", example = "War Remnants Museum")
    private String location;

    @Schema(description = "Vĩ độ")
    private Double latitude;

    @Schema(description = "Kinh độ")
    private Double longitude;

    @Schema(description = "Ngày cập nhật hiển thị UI (dd/MM/yyyy)", example = "20/10/2026")
    private String date;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    private LocalDateTime updatedAt;

    public ArtifactResponse() {
    }

    public ArtifactResponse(Long id, String code, String name, String type, String description, ContentStatus status, String image, Long placeId, String placeName, String location, Double latitude, Double longitude, String date, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type != null ? type : "Hiện vật";
        this.description = description;
        this.status = status;
        this.image = image;
        this.placeId = placeId;
        this.placeName = placeName;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ArtifactResponse fromEntity(Artifact artifact) {
        if (artifact == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = artifact.getUpdatedAt() != null ? artifact.getUpdatedAt().format(formatter)
                : (artifact.getCreatedAt() != null ? artifact.getCreatedAt().format(formatter) : "");

        Long placeId = artifact.getPlace() != null ? artifact.getPlace().getId() : null;
        String placeName = artifact.getPlace() != null ? artifact.getPlace().getName() : null;
        String location = artifact.getPlace() != null ? artifact.getPlace().getName() : "Hiện vật độc lập";

        return new ArtifactResponse(
                artifact.getId(),
                artifact.getCode(),
                artifact.getName(),
                "Hiện vật",
                artifact.getDescription(),
                artifact.getStatus(),
                artifact.getImage(),
                placeId,
                placeName,
                location,
                artifact.getLatitude(),
                artifact.getLongitude(),
                formattedDate,
                artifact.getCreatedAt(),
                artifact.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public void setStatus(ContentStatus status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
