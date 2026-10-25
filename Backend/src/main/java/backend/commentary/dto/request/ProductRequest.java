package backend.commentary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu thêm mới hoặc cập nhật Sản phẩm")
public class ProductRequest {

    @Schema(description = "Mã sản phẩm (tự sinh nếu để trống)", example = "SP-0001")
    private String code;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Schema(description = "Tên sản phẩm", example = "Vé tham quan Bảo tàng")
    private String name;

    @Schema(description = "Mô tả sản phẩm")
    private String description;

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn giá phải lớn hơn hoặc bằng 0")
    @Schema(description = "Đơn giá bán", example = "500000.0")
    private Double price;

    @Min(value = 0, message = "Tồn kho phải lớn hơn hoặc bằng 0")
    @Schema(description = "Số lượng tồn kho", example = "100")
    private Long stock = 0L;

    @Schema(description = "ID địa điểm/bảo tàng liên quan (tùy chọn)")
    private Long placeId;

    @Schema(description = "Sản phẩm có đang bán hay không", example = "true")
    private Boolean active = true;

    public ProductRequest() {
    }

    public ProductRequest(String code, String name, String description, Double price, Long stock, Long placeId, Boolean active) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock != null ? stock : 0L;
        this.placeId = placeId;
        this.active = active != null ? active : true;
    }

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

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String code;
        private String name;
        private String description;
        private Double price;
        private Long stock = 0L;
        private Long placeId;
        private Boolean active = true;

        public Builder code(String code) { this.code = code; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder price(Double price) { this.price = price; return this; }
        public Builder stock(Long stock) { this.stock = stock; return this; }
        public Builder placeId(Long placeId) { this.placeId = placeId; return this; }
        public Builder active(Boolean active) { this.active = active; return this; }

        public ProductRequest build() {
            return new ProductRequest(code, name, description, price, stock, placeId, active);
        }
    }
}
