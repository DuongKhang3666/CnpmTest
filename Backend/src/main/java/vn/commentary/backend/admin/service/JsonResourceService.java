package vn.commentary.backend.admin.service;

import vn.commentary.backend.common.exception.ResourceNotFoundException;
import vn.commentary.backend.admin.repository.JsonResourceRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JsonResourceService {
    private final JsonResourceRepository repository;

    public JsonResourceService(JsonResourceRepository repository) { this.repository = repository; }

    public List<Map<String, Object>> list(String resource, Map<String, String> filters) {
        return repository.findAll(resource, filters);
    }

    public Object getCollection(String resource, Map<String, String> filters) {
        List<Map<String, Object>> rows = list(resource, filters);
        if ("settings".equals(resource)) {
            return rows.isEmpty() ? Map.of("accessHours", 24) : rows.get(0);
        }
        return rows;
    }

    public Map<String, Object> getById(String resource, String id) {
        return repository.findById(resource, id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy " + resource + " với id=" + id));
    }

    public Map<String, Object> create(String resource, Map<String, Object> data) {
        return repository.insert(resource, data == null ? new LinkedHashMap<>() : data);
    }

    public Map<String, Object> update(String resource, String id, Map<String, Object> data) {
        return repository.update(resource, id, data == null ? new LinkedHashMap<>() : data);
    }

    public void delete(String resource, String id) { repository.delete(resource, id); }
}
