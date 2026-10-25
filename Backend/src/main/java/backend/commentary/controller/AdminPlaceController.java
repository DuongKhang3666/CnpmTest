package backend.commentary.controller;

import backend.commentary.dto.request.PlaceRequest;
import backend.commentary.dto.request.StatusUpdateRequest;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.dto.response.PlaceResponse;
import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.entity.enums.PlaceType;
import backend.commentary.service.PlaceService;
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
@RequestMapping("/api/admin/places")
@Tag(name = "Quản lý Địa điểm & Bảo tàng (Admin)", description = "APIs CRUD, tìm kiếm, lọc, phân trang và đổi trạng thái duyệt Địa điểm / Bảo tàng")
public class AdminPlaceController {

    private final PlaceService placeService;

    public AdminPlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách Địa điểm & Bảo tàng", description = "Hỗ trợ tìm kiếm theo từ khóa 'q', lọc theo loại 'type', lọc theo trạng thái 'status' và phân trang")
    public ResponseEntity<PageResponse<PlaceResponse>> getPlaces(
            @Parameter(description = "Từ khóa tìm kiếm (tên hoặc mã)") @RequestParam(required = false) String q,
            @Parameter(description = "Loại ('Địa điểm' hoặc 'Bảo tàng')") @RequestParam(required = false) PlaceType type,
            @Parameter(description = "Trạng thái ('Đã xuất bản', 'Chờ duyệt', 'Bản nháp')") @RequestParam(required = false) ContentStatus status,
            @Parameter(description = "Số trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Số mục trên mỗi trang") @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<PlaceResponse> response = placeService.getPlaces(q, type, status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết Địa điểm / Bảo tàng theo ID")
    public ResponseEntity<PlaceResponse> getPlaceById(@PathVariable Long id) {
        PlaceResponse response = placeService.getPlaceById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Tạo mới Địa điểm / Bảo tàng", description = "Tự động sinh mã định danh DD-xxx hoặc BT-xxx tùy theo loại nội dung")
    public ResponseEntity<PlaceResponse> createPlace(
            @Valid @RequestBody PlaceRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String adminEmail = authentication.getName();
        PlaceResponse response = placeService.createPlace(request, adminEmail, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật toàn bộ thông tin Địa điểm / Bảo tàng")
    public ResponseEntity<PlaceResponse> updatePlace(
            @PathVariable Long id,
            @Valid @RequestBody PlaceRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String adminEmail = authentication.getName();
        PlaceResponse response = placeService.updatePlace(id, request, adminEmail, httpRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa Địa điểm / Bảo tàng", description = "Xóa trực tiếp khỏi database, tự động xóa kèm tất cả các hiện vật thuộc về địa điểm này")
    public ResponseEntity<Map<String, String>> deletePlace(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String adminEmail = authentication.getName();
        placeService.deletePlace(id, adminEmail, httpRequest);
        return ResponseEntity.ok(Map.of("message", "Đã xóa địa điểm và các hiện vật liên quan thành công."));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Duyệt / Đổi trạng thái xuất bản", description = "Cập nhật trạng thái thành 'Đã xuất bản', 'Chờ duyệt' hoặc 'Bản nháp'")
    public ResponseEntity<PlaceResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String adminEmail = authentication.getName();
        PlaceResponse response = placeService.updateStatus(id, request.getStatus(), adminEmail, httpRequest);
        return ResponseEntity.ok(response);
    }
}
