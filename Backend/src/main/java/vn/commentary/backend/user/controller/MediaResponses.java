package vn.commentary.backend.user.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import vn.commentary.backend.exception.ResourceNotFoundException;
import vn.commentary.backend.user.util.DataUrl;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/** Biến giá trị lưu trong DB (data URL hoặc URL ngoài) thành phản hồi nhị phân/chuyển hướng. */
final class MediaResponses {
    private MediaResponses() {
    }

    static ResponseEntity<byte[]> of(String stored) {
        if (stored == null || stored.isBlank()) throw new ResourceNotFoundException("Không có dữ liệu.");
        if (DataUrl.isDataUrl(stored)) {
            DataUrl.Parsed parsed;
            try {
                parsed = DataUrl.parse(stored);
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Dữ liệu tệp không hợp lệ.");
            }
            MediaType type;
            try {
                type = MediaType.parseMediaType(parsed.mime());
            } catch (RuntimeException e) {
                type = MediaType.APPLICATION_OCTET_STREAM;
            }
            return ResponseEntity.ok()
                    .contentType(type)
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                    .body(parsed.bytes());
        }
        try {
            return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, URI.create(stored.trim()).toString())
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Đường dẫn tệp không hợp lệ.");
        }
    }
}
