package backend.commentary;

import backend.commentary.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "app.upload.dir=${java.io.tmpdir}/test_uploads"
})
public class MediaServiceTest {

    @Autowired
    private MediaService mediaService;

    @TempDir
    Path tempDir;

    private MultipartFile createImageFile(String name, String contentType, byte[] content) {
        return new MockMultipartFile("files", name, contentType, content);
    }

    private byte[] validJpegBytes() {
        // Minimal valid JPEG header
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00};
    }

    @Test
    @DisplayName("Upload 1 file ảnh hợp lệ -> trả về URL")
    void testUploadSingleValidImage() {
        MultipartFile file = createImageFile("test.jpg", "image/jpeg", validJpegBytes());

        String url = mediaService.uploadSingle(file);

        assertThat(url).startsWith("/uploads/");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    @DisplayName("Upload nhiều file ảnh hợp lệ -> trả về danh sách URL")
    void testUploadMultipleValidImages() {
        MultipartFile file1 = createImageFile("test1.jpg", "image/jpeg", validJpegBytes());
        MultipartFile file2 = createImageFile("test2.png", "image/png", validJpegBytes());

        List<String> urls = mediaService.uploadMultiple(List.of(file1, file2));

        assertThat(urls).hasSize(2);
        assertThat(urls.get(0)).startsWith("/uploads/");
        assertThat(urls.get(1)).startsWith("/uploads/");
    }

    @Test
    @DisplayName("Upload file không phải ảnh -> ném IllegalArgumentException")
    void testUploadNonImageFile() {
        MultipartFile file = createImageFile("test.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> mediaService.uploadSingle(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chỉ chấp nhận file ảnh");
    }

    @Test
    @DisplayName("Upload file vượt quá 10MB -> ném IllegalArgumentException")
    void testUploadOversizedFile() {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile file = createImageFile("large.jpg", "image/jpeg", largeContent);

        assertThatThrownBy(() -> mediaService.uploadSingle(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vượt quá 10MB");
    }

    @Test
    @DisplayName("Upload quá 10 file cùng lúc -> ném IllegalArgumentException")
    void testUploadTooManyFiles() {
        List<MultipartFile> files = List.of(
                createImageFile("1.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("2.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("3.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("4.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("5.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("6.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("7.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("8.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("9.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("10.jpg", "image/jpeg", validJpegBytes()),
                createImageFile("11.jpg", "image/jpeg", validJpegBytes())
        );

        assertThatThrownBy(() -> mediaService.uploadMultiple(files))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tối đa 10 ảnh");
    }

    @Test
    @DisplayName("Xóa ảnh theo URL tồn tại -> trả về true")
    void testDeleteExistingImage() {
        MultipartFile file = createImageFile("delete_me.jpg", "image/jpeg", validJpegBytes());
        String url = mediaService.uploadSingle(file);

        boolean deleted = mediaService.deleteByUrl(url);

        assertThat(deleted).isTrue();
        // Xóa lần 2 -> false vì file đã không còn
        assertThat(mediaService.deleteByUrl(url)).isFalse();
    }

    @Test
    @DisplayName("Xóa ảnh URL không hợp lệ -> trả về false")
    void testDeleteInvalidUrl() {
        assertThat(mediaService.deleteByUrl("invalid-url")).isFalse();
        assertThat(mediaService.deleteByUrl("/uploads/nonexistent.jpg")).isFalse();
        assertThat(mediaService.deleteByUrl(null)).isFalse();
    }
}