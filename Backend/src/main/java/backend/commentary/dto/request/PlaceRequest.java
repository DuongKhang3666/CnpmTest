package backend.commentary.dto.request;

import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.entity.enums.PlaceCategory;
import backend.commentary.entity.enums.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Yêu cầu thêm mới hoặc cập nhật Địa điểm / Bảo tàng")
public class PlaceRequest {

    @NotBlank(message = "Tên địa điểm không được để trống")
    @Schema(description = "Tên địa điểm hoặc bảo tàng", example = "Independence Palace")
    private String name;

    @NotNull(message = "Loại nội dung không được để trống ('Địa điểm' hoặc 'Bảo tàng')")
    @Schema(description = "Loại: 'Địa điểm' hoặc 'Bảo tàng'", example = "Địa điểm")
    private PlaceType type;

    @NotNull(message = "Phân loại không được để trống")
    @Schema(description = "Phân loại", example = "Di tích lịch sử")
    private PlaceCategory category;

    @NotBlank(message = "Mô tả không được để trống")
    @Schema(description = "Mô tả chi tiết nội dung", example = "Dinh Độc Lập là một di tích lịch sử nổi tiếng tại TP. Hồ Chí Minh...")
    private String description;

    @Schema(description = "Địa chỉ", example = "135 Nam Kỳ Khởi Nghĩa, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh")
    private String address;

    @DecimalMin(value = "-90.0", message = "Vĩ độ tối thiểu là -90")
    @DecimalMax(value = "90.0", message = "Vĩ độ tối đa là 90")
    @Schema(description = "Vĩ độ (-90 đến 90)", example = "10.7770")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Kinh độ tối thiểu là -180")
    @DecimalMax(value = "180.0", message = "Kinh độ tối đa là 180")
    @Schema(description = "Kinh độ (-180 đến 180)", example = "106.6953")
    private Double longitude;

    @Schema(description = "Trạng thái ('Đã xuất bản', 'Chờ duyệt', 'Bản nháp')", example = "Đã xuất bản")
    private ContentStatus status = ContentStatus.PENDING;

    @Schema(description = "URL ảnh đại diện chính", example = "https://images.unsplash.com/photo-1565967511849-76a60a516170")
    private String image;

    @Schema(description = "Danh sách URL ảnh phụ (tối đa 9 ảnh)")
    private List<String> extraImages = new ArrayList<>();

    public PlaceRequest() {
    }

    public PlaceRequest(String name, PlaceType type, PlaceCategory category, String description, String address, Double latitude, Double longitude, ContentStatus status, String image, List<String> extraImages) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status != null ? status : ContentStatus.PENDING;
        this.image = image;
        this.extraImages = extraImages != null ? extraImages : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlaceType getType() {
        return type;
    }

    public void setType(PlaceType type) {
        this.type = type;
    }

    public PlaceCategory getCategory() {
        return category;
    }

    public void setCategory(PlaceCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public List<String> getExtraImages() {
        return extraImages;
    }

    public void setExtraImages(List<String> extraImages) {
        this.extraImages = extraImages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private PlaceType type;
        private PlaceCategory category;
        private String description;
        private String address;
        private Double latitude;
        private Double longitude;
        private ContentStatus status = ContentStatus.PENDING;
        private String image;
        private List<String> extraImages = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(PlaceType type) {
            this.type = type;
            return this;
        }

        public Builder category(PlaceCategory category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
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

        public Builder status(ContentStatus status) {
            this.status = status;
            return this;
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Builder extraImages(List<String> extraImages) {
            this.extraImages = extraImages;
            return this;
        }

        public PlaceRequest build() {
            return new PlaceRequest(name, type, category, description, address, latitude, longitude, status, image, extraImages);
        }
    }
}
