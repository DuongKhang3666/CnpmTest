package backend.commentary.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Trạng thái nội dung (khớp đúng 100% với chuỗi hiển thị trên Frontend)
 */
public enum ContentStatus {
    PUBLISHED("Đã xuất bản"),
    PENDING("Chờ duyệt"),
    DRAFT("Bản nháp");

    private final String displayName;

    ContentStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static ContentStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ContentStatus status : ContentStatus.values()) {
            if (status.displayName.equalsIgnoreCase(value.trim()) || status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái không hợp lệ: " + value + ". Các giá trị hợp lệ: 'Đã xuất bản', 'Chờ duyệt', 'Bản nháp'");
    }
}
