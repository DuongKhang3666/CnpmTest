package backend.commentary.service;

import backend.commentary.dto.request.ArtifactRequest;
import backend.commentary.dto.response.ArtifactResponse;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.entity.Artifact;
import backend.commentary.entity.Place;
import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.exception.ResourceNotFoundException;
import backend.commentary.repository.ArtifactRepository;
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
 * Service xử lý toàn bộ nghiệp vụ Quản lý Hiện vật (Artifact)
 */
@Service
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final PlaceRepository placeRepository;
    private final AuditLogService auditLogService;

    public ArtifactService(ArtifactRepository artifactRepository, PlaceRepository placeRepository, AuditLogService auditLogService) {
        this.artifactRepository = artifactRepository;
        this.placeRepository = placeRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Lấy danh sách hiện vật có tìm kiếm, lọc theo địa điểm, lọc theo trạng thái và phân trang
     * @param page Trang hiện tại (1-indexed)
     * @param size Số phần tử mỗi trang
     */
    @Transactional(readOnly = true)
    public PageResponse<ArtifactResponse> getArtifacts(String query, Long placeId, ContentStatus status, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, size);

        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Artifact> artifactPage = artifactRepository.searchArtifacts(
                query != null ? query.trim() : null,
                placeId,
                status,
                pageable
        );

        List<ArtifactResponse> items = artifactPage.getContent().stream()
                .map(ArtifactResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                items,
                page,
                pageSize,
                artifactPage.getTotalElements(),
                Math.max(1, artifactPage.getTotalPages())
        );
    }

    /**
     * Lấy chi tiết hiện vật theo ID
     */
    @Transactional(readOnly = true)
    public ArtifactResponse getArtifactById(Long id) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hiện vật với ID: " + id));
        return ArtifactResponse.fromEntity(artifact);
    }

    /**
     * Thêm mới hiện vật (tự sinh mã định danh HV-xxx)
     */
    @Transactional
    public ArtifactResponse createArtifact(ArtifactRequest request, String adminEmail, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Place place = null;
        if (request.getPlaceId() != null) {
            place = placeRepository.findById(request.getPlaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Địa điểm liên kết không tồn tại với ID: " + request.getPlaceId()));
        }

        // Tọa độ: nếu người dùng không nhập và có place thì tự kế thừa tọa độ của place
        Double lat = request.getLatitude();
        Double lng = request.getLongitude();
        if (lat == null && lng == null && place != null) {
            lat = place.getLatitude();
            lng = place.getLongitude();
        }

        String generatedCode = generateNextCode();

        Artifact artifact = Artifact.builder()
                .code(generatedCode)
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .status(request.getStatus() != null ? request.getStatus() : ContentStatus.PENDING)
                .image(request.getImage())
                .latitude(lat)
                .longitude(lng)
                .place(place)
                .build();

        Artifact saved = artifactRepository.save(artifact);

        // Audit log
        Map<String, Object> createDetails = new HashMap<>();
        createDetails.put("code", saved.getCode());
        createDetails.put("placeId", saved.getPlace() != null ? saved.getPlace().getId() : null);
        createDetails.put("status", saved.getStatus().getDisplayName());
        auditLogService.log(adminEmail, "CREATE", "ARTIFACT", saved.getId(), saved.getName(),
                createDetails, httpRequest);

        return ArtifactResponse.fromEntity(saved);
    }

    /**
     * Cập nhật thông tin hiện vật
     */
    @Transactional
    public ArtifactResponse updateArtifact(Long id, ArtifactRequest request, String adminEmail, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hiện vật với ID: " + id));

        Place place = null;
        if (request.getPlaceId() != null) {
            place = placeRepository.findById(request.getPlaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Địa điểm liên kết không tồn tại với ID: " + request.getPlaceId()));
        }

        // Capture old values
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("name", artifact.getName());
        oldValues.put("placeId", artifact.getPlace() != null ? artifact.getPlace().getId() : null);
        oldValues.put("status", artifact.getStatus().getDisplayName());
        oldValues.put("image", artifact.getImage());

        artifact.setName(request.getName().trim());
        artifact.setDescription(request.getDescription().trim());
        if (request.getStatus() != null) {
            artifact.setStatus(request.getStatus());
        }
        artifact.setImage(request.getImage());
        artifact.setLatitude(request.getLatitude());
        artifact.setLongitude(request.getLongitude());
        artifact.setPlace(place);

        Artifact updated = artifactRepository.save(artifact);

        // Audit log
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("name", updated.getName());
        newValues.put("placeId", updated.getPlace() != null ? updated.getPlace().getId() : null);
        newValues.put("status", updated.getStatus().getDisplayName());
        newValues.put("image", updated.getImage());

        auditLogService.log(adminEmail, "UPDATE", "ARTIFACT", updated.getId(), updated.getName(),
                Map.of("old", oldValues, "new", newValues), httpRequest);

        return ArtifactResponse.fromEntity(updated);
    }

    /**
     * Xóa hiện vật khỏi database
     */
    @Transactional
    public void deleteArtifact(Long id, String adminEmail, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hiện vật với ID: " + id));

        String name = artifact.getName();
        Long artifactId = artifact.getId();
        artifactRepository.delete(artifact);

        // Audit log
        auditLogService.log(adminEmail, "DELETE", "ARTIFACT", artifactId, name,
                Map.of("deleted", true), httpRequest);
    }

    /**
     * Cập nhật trạng thái duyệt / xuất bản ('Đã xuất bản', 'Chờ duyệt', 'Bản nháp')
     */
    @Transactional
    public ArtifactResponse updateStatus(Long id, ContentStatus status, String adminEmail, jakarta.servlet.http.HttpServletRequest httpRequest) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hiện vật với ID: " + id));

        ContentStatus oldStatus = artifact.getStatus();
        artifact.setStatus(status);
        Artifact updated = artifactRepository.save(artifact);

        // Audit log
        auditLogService.log(adminEmail, "STATUS_CHANGE", "ARTIFACT", updated.getId(), updated.getName(),
                Map.of("oldStatus", oldStatus.getDisplayName(), "newStatus", status.getDisplayName()), httpRequest);

        return ArtifactResponse.fromEntity(updated);
    }

    /**
     * Tự động sinh mã định danh dạng HV-001, HV-002...
     */
    private synchronized String generateNextCode() {
        long count = artifactRepository.count() + 1;
        String code = String.format("HV-%03d", count);

        while (artifactRepository.existsByCode(code)) {
            count++;
            code = String.format("HV-%03d", count);
        }
        return code;
    }
}
