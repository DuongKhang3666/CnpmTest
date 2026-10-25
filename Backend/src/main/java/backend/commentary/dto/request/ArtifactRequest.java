package backend.commentary.dto.request;

import backend.commentary.entity.enums.ContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu thêm mới hoặc cập nhật Hiện vật (Artifact)")
public class ArtifactRequest {

    @NotBlank(message = "Tên hiện vật không được để trống")
    @Schema(description = "Tên hiện vật", example = "Xe Tăng 390")
    private String name;

    @NotBlank(message = "Mô tả hiện vật không được để trống")
    @Schema(description = "Mô tả lịch sử, nguồn gốc hiện vật", example = "Xe tăng húc đổ cổng Dinh Độc Lập ngày 30/4/1975...")
    private String description;

    @Schema(description = "ID của địa điểm / bảo tàng chứa hiện vật (để trống nếu là hiện vật độc lập)", example = "1")
    private Long placeId;

    @Schema(description = "Trạng thái ('Đã xuất bản', 'Chờ duyệt', 'Bản nháp')", example = "Đã xuất bản")
    private ContentStatus status = ContentStatus.PENDING;

    @Schema(description = "URL ảnh hiện vật", example = "https://images.unsplash.com/photo-tank")
    private String image;

    @DecimalMin(value = "-90.0", message = "Vĩ độ tối thiểu là -90")
    @DecimalMax(value = "90.0", message = "Vĩ độ tối đa là 90")
    @Schema(description = "Vĩ độ tọa độ hiện vật (nếu có)", example = "10.7770")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Kinh độ tối thiểu là -180")
    @DecimalMax(value = "180.0", message = "Kinh độ tối đa là 180")
    @Schema(description = "Kinh độ tọa độ hiện vật (nếu có)", example = "106.6953")
    private Double longitude;

    public ArtifactRequest() {
    }

    public ArtifactRequest(String name, String description, Long placeId, ContentStatus status, String image, Double latitude, Double longitude) {
        this.name = name;
        this.description = description;
        this.placeId = placeId;
        this.status = status != null ? status : ContentStatus.PENDING;
        this.image = image;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private Long placeId;
        private ContentStatus status = ContentStatus.PENDING;
        private String image;
        private Double latitude;
        private Double longitude;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder placeId(Long placeId) {
            this.placeId = placeId;
            return this;
        }

        public Builder status(ContentStatus status) {
            this.status = status;
            return this;
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public ArtifactRequest build() {
            return new ArtifactRequest(name, description, placeId, status, image, latitude, longitude);
        }
    }
}
