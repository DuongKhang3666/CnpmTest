package backend.commentary.service;

import backend.commentary.dto.request.PlaceRequest;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.dto.response.PlaceResponse;
import backend.commentary.entity.Place;
import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.entity.enums.PlaceCategory;
import backend.commentary.entity.enums.PlaceType;
import backend.commentary.exception.ResourceNotFoundException;
import backend.commentary.repository.PlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service xử lý toàn bộ nghiệp vụ Quản lý Địa điểm và Bảo tàng
 */
@Service
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final AuditLogService auditLogService;

    public PlaceService(PlaceRepository placeRepository, AuditLogService auditLogService) {
        this.placeRepository = placeRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Lấy danh sách địa điểm có tìm kiếm, lọc theo loại, lọc theo trạng thái và phân trang
     * @param page Trang hiện tại (1-indexed từ client)
     * @param size Số phần tử mỗi trang
     */
    @Transactional(readOnly = true)
    public PageResponse<PlaceResponse> getPlaces(String query, PlaceType type, ContentStatus status, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, size);

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Place> placePage = placeRepository.searchPlaces(query != null ? query.trim() : null, type, status, pageable);

        List<PlaceResponse> items = placePage.getContent().stream()
                .map(PlaceResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                items,
                page,
                pageSize,
                placePage.getTotalElements(),
                Math.max(1, placePage.getTotalPages())
        );
    }

    /**
     * Lấy chi tiết Địa điểm / Bảo tàng theo ID
     */
    @Transactional(readOnly = true)
    public PlaceResponse getPlaceById(Long id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm / bảo tàng với ID: " + id));
        return PlaceResponse.fromEntity(place);
    }

    /**
     * Thêm mới Địa điểm / Bảo tàng (tự sinh mã DD-xxx hoặc BT-xxx)
     */
    @Transactional
    public PlaceResponse createPlace(PlaceRequest request, String adminEmail, jakarta.servlet.http.HttpServletRequest httpRequest) {
        validateTypeAndCategory(request.getType(), request.getCategory());
        if (request.getExtraImages() != null && request.getExtraImages().size() > 9) {
            throw new IllegalArgumentException("Chỉ được thêm tối đa 9 ảnh phụ.");
        }

        String generatedCode = generateNextCode(request.getType());

        Place place = Place.builder()
                .code(generatedCode)
                .name(request.getName().trim())
                .type(request.getType())
                .category(request.getCategory())
                .description(request.getDescription().trim())
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(request.getStatus() != null ? request.getStatus() : ContentStatus.PENDING)
                .image(request.getImage())
                .extraImages(request.getExtraImages())
                .build();

        Place saved = placeRepository.save(place);

        // Audit log
        Map<String, Object> createDetails = new HashMap<>();
        createDetails.put("code", saved.getCode());
        createDetails.put("type", saved.getType().getDisplayName());
        createDetails.put("category", saved.getCategory().getDisplayName());
        createDetails.put("status", saved.getStatus().getDisplayName());
        auditLogService.log(adminEmail, "CREATE", "PLACE", saved.getId(), saved.getName(),
                createDetails, httpRequest);

        return PlaceResponse.fromEntity(saved);
    }

    /**
     * Cập nhật thông tin Địa điểm / Bảo tàng
     */
    @Transactional
    public PlaceResponse updatePlace(Long id, PlaceRequest request, String adminEmail, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm / bảo tàng với ID: " + id));

        validateTypeAndCategory(request.getType(), request.getCategory());
        if (request.getExtraImages() != null && request.getExtraImages().size() > 9) {
            throw new IllegalArgumentException("Chỉ được thêm tối đa 9 ảnh phụ.");
        }

        // Capture old values for audit
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("name", place.getName());
        oldValues.put("type", place.getType().getDisplayName());
        oldValues.put("category", place.getCategory().getDisplayName());
        oldValues.put("status", place.getStatus().getDisplayName());
        oldValues.put("image", place.getImage());
        oldValues.put("extraImages", place.getExtraImages());

        place.setName(request.getName().trim());
        place.setType(request.getType());
        place.setCategory(request.getCategory());
        place.setDescription(request.getDescription().trim());
        place.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        place.setLatitude(request.getLatitude());
        place.setLongitude(request.getLongitude());
        if (request.getStatus() != null) {
            place.setStatus(request.getStatus());
        }
        place.setImage(request.getImage());
        if (request.getExtraImages() != null) {
            place.setExtraImages(request.getExtraImages());
        }

        Place updated = placeRepository.save(place);

        // Audit log
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("name", updated.getName());
        newValues.put("type", updated.getType().getDisplayName());
        newValues.put("category", updated.getCategory().getDisplayName());
        newValues.put("status", updated.getStatus().getDisplayName());
        newValues.put("image", updated.getImage());
        newValues.put("extraImages", updated.getExtraImages());

        auditLogService.log(adminEmail, "UPDATE", "PLACE", updated.getId(), updated.getName(),
                Map.of("old", oldValues, "new", newValues), httpRequest);

        return PlaceResponse.fromEntity(updated);
    }

    /**
     * Xóa Địa điểm / Bảo tàng (xóa luôn các hiện vật thuộc địa điểm này)
     */
    @Transactional
    public void deletePlace(Long id, String adminEmail, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm / bảo tàng với ID: " + id));

        String name = place.getName();
        Long placeId = place.getId();
        placeRepository.delete(place);

        // Audit log
        auditLogService.log(adminEmail, "DELETE", "PLACE", placeId, name,
                Map.of("deleted", true), httpRequest);
    }

    /**
     * Đổi trạng thái duyệt / xuất bản ('Đã xuất bản', 'Chờ duyệt', 'Bản nháp')
     */
    @Transactional
    public PlaceResponse updateStatus(Long id, ContentStatus status, String adminEmail, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm / bảo tàng với ID: " + id));

        ContentStatus oldStatus = place.getStatus();
        place.setStatus(status);
        Place updated = placeRepository.save(place);

        // Audit log
        auditLogService.log(adminEmail, "STATUS_CHANGE", "PLACE", updated.getId(), updated.getName(),
                Map.of("oldStatus", oldStatus.getDisplayName(), "newStatus", status.getDisplayName()), httpRequest);

        return PlaceResponse.fromEntity(updated);
    }

    /**
     * Kiểm tra tính nhất quán giữa Loại (type) và Phân loại (category):
     * - Bảo tàng (BAO_TANG) chỉ được đi kèm category = Bảo tàng (BAO_TANG)
     * - Địa điểm (DIA_DIEM) không được dùng category = Bảo tàng
     */
    private void validateTypeAndCategory(PlaceType type, PlaceCategory category) {
        if (type == PlaceType.BAO_TANG && category != PlaceCategory.BAO_TANG) {
            throw new IllegalArgumentException("Bảo tàng chỉ được có phân loại 'Bảo tàng' (nhận: " + category.getDisplayName() + ")");
        }
        if (type == PlaceType.DIA_DIEM && category == PlaceCategory.BAO_TANG) {
            throw new IllegalArgumentException("Địa điểm không được có phân loại 'Bảo tàng'");
        }
    }

    /**
     * Tự động sinh mã định danh theo loại:
     * - Địa điểm: DD-001, DD-002, ...
     * - Bảo tàng: BT-001, BT-002, ...
     */
    private synchronized String generateNextCode(PlaceType type) {
        String prefix = type.getPrefix();
        long count = placeRepository.countByType(type) + 1;
        String code = String.format("%s-%03d", prefix, count);

        // Đảm bảo không trùng mã nếu có bản ghi bị xóa trước đó
        while (placeRepository.existsByCode(code)) {
            count++;
            code = String.format("%s-%03d", prefix, count);
        }
        return code;
    }
}
