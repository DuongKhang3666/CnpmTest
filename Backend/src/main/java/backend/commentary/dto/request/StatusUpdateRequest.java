package backend.commentary.dto.request;

import backend.commentary.entity.enums.ContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu thay đổi trạng thái duyệt / xuất bản")
public class StatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    @Schema(description = "Trạng thái mới: 'Đã xuất bản', 'Chờ duyệt', 'Bản nháp'", example = "Đã xuất bản")
    private ContentStatus status;

    public StatusUpdateRequest() {
    }

    public StatusUpdateRequest(ContentStatus status) {
        this.status = status;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public void setStatus(ContentStatus status) {
        this.status = status;
    }
}
