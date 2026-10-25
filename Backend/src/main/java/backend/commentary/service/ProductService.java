package backend.commentary.service;

import backend.commentary.dto.request.ProductRequest;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.dto.response.ProductResponse;
import backend.commentary.entity.Product;
import backend.commentary.entity.Place;
import backend.commentary.exception.ResourceNotFoundException;
import backend.commentary.repository.PlaceRepository;
import backend.commentary.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service quản lý Sản phẩm (Admin)
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final PlaceRepository placeRepository;

    public ProductService(ProductRepository productRepository, PlaceRepository placeRepository) {
        this.productRepository = productRepository;
        this.placeRepository = placeRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(String query, Boolean active, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> productPage = productRepository.searchProducts(query, active, pageable);

        List<ProductResponse> items = productPage.getContent().stream()
                .map(ProductResponse::fromEntity)
                .toList();

        return new PageResponse<>(items, page, pageSize, productPage.getTotalElements(),
                Math.max(1, productPage.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setCode(request.getCode() != null && !request.getCode().isBlank()
                ? request.getCode() : generateCode());
        applyRequest(product, request);
        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
        applyRequest(product, request);
        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
        productRepository.delete(product);
    }

    /**
     * Dùng nội bộ bởi OrderService để trừ/hoàn tồn kho
     */
    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock() != null ? request.getStock() : product.getStock());
        product.setActive(request.getActive() != null ? request.getActive() : true);
        if (request.getPlaceId() != null) {
            Place place = placeRepository.findById(request.getPlaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm với id: " + request.getPlaceId()));
            product.setPlace(place);
        } else {
            product.setPlace(null);
        }
    }

    private String generateCode() {
        String code = "SP-" + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        while (productRepository.existsByCode(code)) {
            code = "SP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return code;
    }
}
