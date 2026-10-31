package vn.commentary.backend.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.commentary.backend.user.dto.Dtos.Catalog;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.security.SessionInterceptor;
import vn.commentary.backend.user.service.CatalogService;

/** Dữ liệu khởi tạo cho trang hành khách (cần phiên hợp lệ). */
@RestController
@RequestMapping("/api/user")
public class UserCatalogController {
    private final CatalogService catalog;

    public UserCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/catalog")
    public Catalog catalog(@RequestAttribute(SessionInterceptor.ATTR) DeviceSession session) {
        return catalog.build(session);
    }
}
