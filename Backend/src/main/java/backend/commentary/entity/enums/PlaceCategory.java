package backend.commentary.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Phân loại chi tiết của Địa điểm / Bảo tàng
 */
public enum PlaceCategory {
    BAO_TANG("Bảo tàng"),
    DIA_DIEM_DU_LICH("Địa điểm du lịch"),
    DI_TICH_LICH_SU("Di tích lịch sử"),
    DI_SAN_VAN_HOA("Di sản văn hóa"),
    DANH_LAM_THANG_CANH("Danh lam thắng cảnh");

    private final String displayName;

    PlaceCategory(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static PlaceCategory fromString(String value) {
        if (value == null) {
            return null;
        }
        for (PlaceCategory category : PlaceCategory.values()) {
            if (category.displayName.equalsIgnoreCase(value.trim()) || category.name().equalsIgnoreCase(value.trim())) {
                return category;
            }
        }
        throw new IllegalArgumentException("Phân loại không hợp lệ: " + value + ". Các giá trị hợp lệ: 'Bảo tàng', 'Địa điểm du lịch', 'Di tích lịch sử', 'Di sản văn hóa', 'Danh lam thắng cảnh'");
    }
}
