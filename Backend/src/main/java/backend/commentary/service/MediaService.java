package backend.commentary.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service xử lý upload và quản lý file ảnh
 * - Lưu file vào thư mục cục bộ (configurable qua app.upload.dir)
 * - Validate loại file (chỉ nhận ảnh), kích thước (≤ 10MB)
 * - Trả về URL truy cập file (static path /uploads/...)
 */
@Service
public class MediaService {

    private final Path uploadRoot;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public MediaService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục upload: " + this.uploadRoot, e);
        }
    }

    /**
     * Upload nhiều file ảnh cùng lúc (1 ảnh chính + tối đa 9 ảnh phụ)
     * @param files Danh sách file ảnh
     * @return Danh sách URL truy cập file
     */
    public List<String> uploadMultiple(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        if (files.size() > 10) {
            throw new IllegalArgumentException("Chỉ được upload tối đa 10 ảnh (1 chính + 9 phụ).");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadSingle(file));
        }
        return urls;
    }

    /**
     * Upload 1 file ảnh đơn lẻ
     * @param file File ảnh
     * @return URL truy cập file
     */
    public String uploadSingle(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueName = UUID.randomUUID().toString().substring(0, 8);
        String newFilename = timestamp + "_" + uniqueName + "." + extension;

        // Tạo thư mục con theo ngày để dễ quản lý
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path datePath = uploadRoot.resolve(dateFolder);
        try {
            Files.createDirectories(datePath);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục ngày: " + datePath, e);
        }

        Path targetPath = datePath.resolve(newFilename);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file: " + newFilename, e);
        }

        // Trả về đường dẫn static để frontend truy cập
        return "/uploads/" + dateFolder + "/" + newFilename;
    }

    /**
     * Xóa file ảnh theo URL (đường dẫn static /uploads/...)
     * @param fileUrl URL file (ví dụ: /uploads/2026/10/22/abc123.jpg)
     * @return true nếu xóa thành công, false nếu file không tồn tại
     */
    public boolean deleteByUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return false;
        }
        try {
            String relativePath = fileUrl.substring("/uploads/".length());
            Path filePath = uploadRoot.resolve(relativePath).normalize();

            // Bảo mật: chắc chắn file nằm trong thư mục upload
            if (!filePath.startsWith(uploadRoot)) {
                return false;
            }

            return Files.deleteIfExists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước file vượt quá 10MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPEG, PNG, WebP, GIF).");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "jpg";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "jpg";
    }
}