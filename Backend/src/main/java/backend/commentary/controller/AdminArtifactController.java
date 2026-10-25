package backend.commentary.controller;

import backend.commentary.dto.request.ArtifactRequest;
import backend.commentary.dto.request.StatusUpdateRequest;
import backend.commentary.dto.response.ArtifactResponse;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.service.ArtifactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/artifacts")
@Tag(name = "Quản lý Hiện vật (Admin)", description = "APIs CRUD, tìm kiếm, lọc theo địa điểm, phân trang và đổi trạng thái duyệt Hiện vật")
public class AdminArtifactController {

    private final ArtifactService artifactService;

    public AdminArtifactController(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách Hiện vật", description = "Tìm kiếm từ khóa 'q', lọc theo địa điểm 'placeId', lọc theo trạng thái 'status' và phân trang")
    public ResponseEntity<PageResponse<ArtifactResponse>> getArtifacts(
            @Parameter(description = "Từ khóa tìm kiếm (tên hoặc mã HV)") @RequestParam(required = false) String q,
            @Parameter(description = "Lọc theo ID địa điểm / bảo tàng chứa hiện vật") @RequestParam(required = false) Long placeId,
            @Parameter(description = "Trạng thái ('Đã xuất bản', 'Chờ duyệt', 'Bản nháp')") @RequestParam(required = false) ContentStatus status,
            @Parameter(description = "Số trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Số mục mỗi trang") @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<ArtifactResponse> response = artifactService.getArtifacts(q, placeId, status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết Hiện vật theo ID")
    public ResponseEntity<ArtifactResponse> getArtifactById(@PathVariable Long id) {
        ArtifactResponse response = artifactService.getArtifactById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Tạo mới Hiện vật", description = "Tự động sinh mã HV-xxx, có thể liên kết địa điểm hoặc để độc lập")
    public ResponseEntity<ArtifactResponse> createArtifact(
            @Valid @RequestBody ArtifactRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String adminEmail = authentication.getName();
        ArtifactResponse response = artifactService.createArtifact(request, adminEmail, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin Hiện vật")
    public ResponseEntity<ArtifactResponse> updateArtifact(
            @PathVariable Long id,
            @Valid @RequestBody ArtifactRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String adminEmail = authentication.getName();
        ArtifactResponse response = artifactService.updateArtifact(id, request, adminEmail, httpRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa Hiện vật khỏi database")
    public ResponseEntity<Map<String, String>> deleteArtifact(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String adminEmail = authentication.getName();
        artifactService.deleteArtifact(id, adminEmail, httpRequest);
        return ResponseEntity.ok(Map.of("message", "Đã xóa hiện vật thành công."));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Duyệt / Đổi trạng thái Hiện vật ('Đã xuất bản', 'Chờ duyệt', 'Bản nháp')")
    public ResponseEntity<ArtifactResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String adminEmail = authentication.getName();
        ArtifactResponse response = artifactService.updateStatus(id, request.getStatus(), adminEmail, httpRequest);
        return ResponseEntity.ok(response);
    }
}
