package backend.commentary.entity;

import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.entity.enums.PlaceCategory;
import backend.commentary.entity.enums.PlaceType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Địa điểm & Bảo tàng (Place)
 * - Quản lý các điểm tham quan, di tích, bảo tàng
 * - CascadeType.ALL tới artifacts: khi xóa Place, tất cả Artifact thuộc Place sẽ tự động bị xóa theo
 * - Sẵn sàng mở rộng quan hệ sang Commentary và CommentaryTranslation
 */
@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlaceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlaceCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String address;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ContentStatus status = ContentStatus.PENDING;

    @Column(name = "image_url", length = 1000)
    private String image;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "place_extra_images", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> extraImages = new ArrayList<>();

    // Xóa Địa điểm thì xóa kèm tất cả Hiện vật thuộc địa điểm đó
    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Artifact> artifacts = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Place() {
    }

    public Place(Long id, String code, String name, PlaceType type, PlaceCategory category, String description, String address, Double latitude, Double longitude, ContentStatus status, String image, List<String> extraImages, List<Artifact> artifacts, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.category = category;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status != null ? status : ContentStatus.PENDING;
        this.image = image;
        this.extraImages = extraImages != null ? extraImages : new ArrayList<>();
        this.artifacts = artifacts != null ? artifacts : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlaceType getType() {
        return type;
    }

    public void setType(PlaceType type) {
        this.type = type;
    }

    public PlaceCategory getCategory() {
        return category;
    }

    public void setCategory(PlaceCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public void setStatus(ContentStatus status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getExtraImages() {
        return extraImages;
    }

    public void setExtraImages(List<String> extraImages) {
        this.extraImages = extraImages;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String code;
        private String name;
        private PlaceType type;
        private PlaceCategory category;
        private String description;
        private String address;
        private Double latitude;
        private Double longitude;
        private ContentStatus status = ContentStatus.PENDING;
        private String image;
        private List<String> extraImages = new ArrayList<>();
        private List<Artifact> artifacts = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(PlaceType type) {
            this.type = type;
            return this;
        }

        public Builder category(PlaceCategory category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder status(ContentStatus status) {
            this.status = status;
            return this;
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Builder extraImages(List<String> extraImages) {
            this.extraImages = extraImages;
            return this;
        }

        public Builder artifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Place build() {
            return new Place(id, code, name, type, category, description, address, latitude, longitude, status, image, extraImages, artifacts, createdAt, updatedAt);
        }
    }
}
