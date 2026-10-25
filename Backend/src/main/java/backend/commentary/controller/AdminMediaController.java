package backend.commentary.controller;

import backend.commentary.dto.response.PageResponse;
import backend.commentary.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
@Tag(name = "Quản lý Ảnh (Admin)", description = "APIs upload ảnh (1 chính + 9 phụ, mỗi ảnh ≤ 10MB, chỉ nhận file ảnh) và xóa ảnh")
public class AdminMediaController {

    private final MediaService mediaService;

    public AdminMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload nhiều ảnh cùng lúc", description = "Tối đa 10 file (1 ảnh chính + 9 ảnh phụ). Mỗi file ≤ 10MB. Chỉ nhận JPEG, PNG, WebP, GIF. Trả về danh sách URL để lưu vào Place/Artifact.")
    public ResponseEntity<Map<String, Object>> uploadImages(
            @Parameter(description = "Danh sách file ảnh (form field: 'files')", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "array", format = "binary")))
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request
    ) {
        List<String> urls = mediaService.uploadMultiple(files);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upload thành công " + urls.size() + " ảnh.");
        response.put("urls", urls);
        response.put("mainImage", urls.isEmpty() ? null : urls.get(0));
        response.put("extraImages", urls.size() > 1 ? urls.subList(1, urls.size()) : List.of());

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload 1 ảnh đơn lẻ", description = "Trả về URL ảnh để gán vào trường 'image' của Place hoặc Artifact")
    public ResponseEntity<Map<String, Object>> uploadSingle(
            @Parameter(description = "File ảnh (form field: 'file')", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file
    ) {
        String url = mediaService.uploadSingle(file);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upload thành công.");
        response.put("url", url);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{fileUrl}")
    @Operation(summary = "Xóa ảnh theo URL", description = "Nhận URL ảnh (ví dụ: /uploads/2026/10/22/abc123.jpg) và xóa file vật lý trên server")
    public ResponseEntity<Map<String, String>> deleteImage(
            @Parameter(description = "URL ảnh (đã được mã hóa URL)", example = "/uploads/2026/10/22/abc123.jpg")
            @PathVariable String fileUrl,
            HttpServletRequest request
    ) {
        // PathVariable tự động decode URL, nhưng nếu fileUrl chứa dấu / thì cần xử lý
        // Spring Boot có thể không match đúng nếu URL chứa /, nên dùng request parameter thay thế
        String decodedUrl = fileUrl;
        if (request.getQueryString() != null && request.getQueryString().contains("url=")) {
            // Fallback: lấy từ query param nếu path variable không hoạt động
            decodedUrl = request.getParameter("url");
        }

        boolean deleted = mediaService.deleteByUrl(decodedUrl);
        Map<String, String> response = new HashMap<>();
        if (deleted) {
            response.put("message", "Đã xóa ảnh thành công.");
        } else {
            response.put("message", "Không tìm thấy ảnh hoặc không thể xóa.");
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Xóa ảnh theo URL (query param)", description = "Dùng query param 'url' thay vì path variable để tránh vấn đề với dấu / trong URL")
    public ResponseEntity<Map<String, String>> deleteImageByQuery(
            @Parameter(description = "URL ảnh", example = "/uploads/2026/10/22/abc123.jpg")
            @RequestParam("url") String fileUrl
    ) {
        boolean deleted = mediaService.deleteByUrl(fileUrl);
        Map<String, String> response = new HashMap<>();
        if (deleted) {
            response.put("message", "Đã xóa ảnh thành công.");
        } else {
            response.put("message", "Không tìm thấy ảnh hoặc không thể xóa.");
        }
        return ResponseEntity.ok(response);
    }
}