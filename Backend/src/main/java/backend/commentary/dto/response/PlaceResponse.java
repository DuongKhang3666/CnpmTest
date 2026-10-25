package backend.commentary.dto.response;

import backend.commentary.entity.Place;
import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.entity.enums.PlaceCategory;
import backend.commentary.entity.enums.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Thông tin chi tiết Địa điểm / Bảo tàng trả về cho Client")
public class PlaceResponse {

    @Schema(description = "ID bản ghi")
    private Long id;

    @Schema(description = "Mã định danh tự sinh (DD-xxx hoặc BT-xxx)", example = "DD-001")
    private String code;

    @Schema(description = "Tên địa điểm / bảo tàng", example = "Independence Palace")
    private String name;

    @Schema(description = "Loại ('Địa điểm' hoặc 'Bảo tàng')", example = "Địa điểm")
    private PlaceType type;

    @Schema(description = "Phân loại", example = "Di tích lịch sử")
    private PlaceCategory category;

    @Schema(description = "Mô tả chi tiết")
    private String description;

    @Schema(description = "Địa chỉ đầy đủ")
    private String address;

    @Schema(description = "Bí danh vị trí cho Frontend (khớp trường location của UI)", example = "Thành phố Hồ Chí Minh")
    private String location;

    @Schema(description = "Vĩ độ")
    private Double latitude;

    @Schema(description = "Kinh độ")
    private Double longitude;

    @Schema(description = "Trạng thái", example = "Đã xuất bản")
    private ContentStatus status;

    @Schema(description = "URL ảnh đại diện chính")
    private String image;

    @Schema(description = "Danh sách URL ảnh phụ")
    private List<String> extraImages = new ArrayList<>();

    @Schema(description = "Ngày cập nhật định dạng hiển thị UI (dd/MM/yyyy)", example = "20/10/2026")
    private String date;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    private LocalDateTime updatedAt;

    public PlaceResponse() {
    }

    public PlaceResponse(Long id, String code, String name, PlaceType type, PlaceCategory category, String description, String address, String location, Double latitude, Double longitude, ContentStatus status, String image, List<String> extraImages, String date, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.category = category;
        this.description = description;
        this.address = address;
        this.location = location != null ? location : address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.image = image;
        this.extraImages = extraImages != null ? extraImages : new ArrayList<>();
        this.date = date;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PlaceResponse fromEntity(Place place) {
        if (place == null) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = place.getUpdatedAt() != null ? place.getUpdatedAt().format(formatter)
                : (place.getCreatedAt() != null ? place.getCreatedAt().format(formatter) : "");

        return new PlaceResponse(
                place.getId(),
                place.getCode(),
                place.getName(),
                place.getType(),
                place.getCategory(),
                place.getDescription(),
                place.getAddress(),
                place.getAddress(), // location map với address
                place.getLatitude(),
                place.getLongitude(),
                place.getStatus(),
                place.getImage(),
                new ArrayList<>(place.getExtraImages()),
                formattedDate,
                place.getCreatedAt(),
                place.getUpdatedAt()
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
