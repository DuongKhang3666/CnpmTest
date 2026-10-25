package backend.commentary.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Loại địa điểm: Địa điểm hoặc Bảo tàng
 */
public enum PlaceType {
    DIA_DIEM("Địa điểm", "DD"),
    BAO_TANG("Bảo tàng", "BT");

    private final String displayName;
    private final String prefix;

    PlaceType(String displayName, String prefix) {
        this.displayName = displayName;
        this.prefix = prefix;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    @JsonCreator
    public static PlaceType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (PlaceType type : PlaceType.values()) {
            if (type.displayName.equalsIgnoreCase(value.trim()) 
                    || type.name().equalsIgnoreCase(value.trim())
                    || type.prefix.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Loại nội dung không hợp lệ: " + value + ". Các giá trị hợp lệ: 'Địa điểm', 'Bảo tàng'");
    }
}
