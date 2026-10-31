package vn.commentary.backend.admin.entity;

import java.util.LinkedHashMap;
import java.util.Map;

/** A JSON-server-compatible resource row; keys intentionally preserve the DB/API field names. */
public class JsonResource {
    private final String resourceName;
    private final Map<String, Object> fields;

    public JsonResource(String resourceName, Map<String, Object> fields) {
        this.resourceName = resourceName;
        this.fields = new LinkedHashMap<>(fields);
    }
    public String getResourceName() { return resourceName; }
    public Map<String, Object> getFields() { return new LinkedHashMap<>(fields); }
}
