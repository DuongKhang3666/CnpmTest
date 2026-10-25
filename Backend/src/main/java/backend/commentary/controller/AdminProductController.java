package backend.commentary.controller;

import backend.commentary.dto.request.ProductRequest;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.dto.response.ProductResponse;
import backend.commentary.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "Quản lý Sản phẩm bán (Admin)", description = "APIs CRUD sản phẩm/vé bán cho khách hàng")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách Sản phẩm", description = "Hỗ trợ tìm kiếm theo từ khóa 'q', lọc 'active' và phân trang")
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @Parameter(description = "Từ khóa tìm kiếm (tên hoặc mã)") @RequestParam(required = false) String q,
            @Parameter(description = "Lọc còn bán hay không") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Số trang (bắt đầu từ 1)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Số mục trên mỗi trang") @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productService.getProducts(q, active, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết Sản phẩm theo ID")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @Operation(summary = "Tạo mới Sản phẩm", description = "Mã sản phẩm tự sinh nếu để trống")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật toàn bộ thông tin Sản phẩm")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa Sản phẩm")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm thành công."));
    }
}
