package vn.commentary.backend.controller;

import vn.commentary.backend.service.JsonResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Single JSON-server-compatible HTTP adapter; business/data access stays in Service and Repository layers. */
@RestController
public class JsonServerController {
    private final JsonResourceService service;
    public JsonServerController(JsonResourceService service) { this.service = service; }

    @GetMapping("/{resource}")
    public Object getCollection(@PathVariable String resource, @RequestParam Map<String, String> query) {
        return service.getCollection(resource, query);
    }

    @GetMapping("/{resource}/{id}")
    public Map<String, Object> getOne(@PathVariable String resource, @PathVariable String id) {
        return service.getById(resource, id);
    }

    @PostMapping("/{resource}")
    public ResponseEntity<Map<String, Object>> create(@PathVariable String resource,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> saved = service.create(resource, body);
        return ResponseEntity.status("settings".equals(resource) ? HttpStatus.OK : HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{resource}/{id}")
    public Map<String, Object> patch(@PathVariable String resource, @PathVariable String id,
                                     @RequestBody(required = false) Map<String, Object> body) {
        return service.update(resource, id, body);
    }

    @PutMapping("/{resource}/{id}")
    public Map<String, Object> put(@PathVariable String resource, @PathVariable String id,
                                   @RequestBody(required = false) Map<String, Object> body) {
        return service.update(resource, id, body);
    }

    @PatchMapping("/{resource}")
    public Map<String, Object> patchSingleton(@PathVariable String resource,
                                               @RequestBody(required = false) Map<String, Object> body) {
        if (!"settings".equals(resource)) throw new IllegalArgumentException("PATCH không có id chỉ được hỗ trợ cho /settings.");
        return service.update(resource, "settings", body);
    }

    @PutMapping("/{resource}")
    public Map<String, Object> putSingleton(@PathVariable String resource,
                                             @RequestBody(required = false) Map<String, Object> body) {
        if (!"settings".equals(resource)) throw new IllegalArgumentException("PUT không có id chỉ được hỗ trợ cho /settings.");
        return service.update(resource, "settings", body);
    }

    @DeleteMapping("/{resource}/{id}")
    public ResponseEntity<Void> delete(@PathVariable String resource, @PathVariable String id) {
        service.delete(resource, id);
        return ResponseEntity.noContent().build();
    }
}
