package backend.commentary.dto.response;

import backend.commentary.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Thông tin Sản phẩm bán cho khách hàng")
public class ProductResponse {

    @Schema(description = "ID sản phẩm")
    private Long id;

    @Schema(description = "Mã sản phẩm")
    private String code;

    @Schema(description = "Tên sản phẩm")
    private String name;

    @Schema(description = "Mô tả sản phẩm")
    private String description;

    @Schema(description = "Đơn giá bán")
    private Double price;

    @Schema(description = "Số lượng tồn kho")
    private Long stock;

    @Schema(description = "ID địa điểm/bảo tàng liên quan")
    private Long placeId;

    @Schema(description = "Tên địa điểm/bảo tàng liên quan")
    private String placeName;

    @Schema(description = "Sản phẩm có đang bán hay không")
    private Boolean active;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product) {
        ProductResponse response = new ProductResponse();
        response.id = product.getId();
        response.code = product.getCode();
        response.name = product.getName();
        response.description = product.getDescription();
        response.price = product.getPrice();
        response.stock = product.getStock();
        response.placeId = product.getPlace() != null ? product.getPlace().getId() : null;
        response.placeName = product.getPlace() != null ? product.getPlace().getName() : null;
        response.active = product.getActive();
        response.createdAt = product.getCreatedAt();
        response.updatedAt = product.getUpdatedAt();
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Long getStock() { return stock; }
    public void setStock(Long stock) { this.stock = stock; }

    public Long getPlaceId() { return placeId; }
    public void setPlaceId(Long placeId) { this.placeId = placeId; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
