package vn.commentary.backend.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.commentary.backend.admin.entity.ColumnDefinition;
import vn.commentary.backend.admin.entity.JsonResource;
import vn.commentary.backend.common.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class JsonResourceRepository {
    private static final Set<String> RESOURCES = Set.of(
            "sites", "artifacts", "narrations", "languages", "listens", "devices", "history", "likes", "settings");

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final Map<String, List<ColumnDefinition>> metadataCache = new HashMap<>();

    public JsonResourceRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public boolean supports(String resource) {
        return RESOURCES.contains(resource);
    }

    public List<Map<String, Object>> findAll(String resource, Map<String, String> filters) {
        requireResource(resource);
        List<ColumnDefinition> columns = columns(resource);
        StringBuilder sql = new StringBuilder("SELECT * FROM `" + resource + "`");
        List<Object> args = new ArrayList<>();
        List<String> clauses = new ArrayList<>();
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String key = filter.getKey();
            if (key.startsWith("_") || key.isBlank())
                continue;
            ColumnDefinition col = findColumn(columns, key);
            if (col == null)
                continue; // json-server ignores query keys that are not DB fields
            clauses.add("`" + col.name() + "` = ?");
            args.add(toDatabaseValue(col, filter.getValue()));
        }
        if (!clauses.isEmpty())
            sql.append(" WHERE ").append(String.join(" AND ", clauses));
        String sort = filters.get("_sort");
        if (sort != null && findColumn(columns, sort) != null) {
            String order = "desc".equalsIgnoreCase(filters.get("_order")) ? "DESC" : "ASC";
            sql.append(" ORDER BY `" + sort + "` " + order);
        } else if (findColumn(columns, "id") != null) {
            sql.append(" ORDER BY `id` ASC");
        }
        List<Map<String, Object>> rows = jdbc.query(sql.toString(), this::mapRow, args.toArray());
        int start = parseInt(filters.get("_start"), 0);
        int end = parseInt(filters.get("_end"), -1);
        int limit = parseInt(filters.get("_limit"), -1);
        if (start > 0 || end >= 0 || limit >= 0) {
            int from = Math.min(Math.max(start, 0), rows.size());
            int to = end >= 0 ? Math.min(end, rows.size())
                    : (limit >= 0 ? Math.min(from + limit, rows.size()) : rows.size());
            return new ArrayList<>(rows.subList(from, Math.max(from, to)));
        }
        return rows;
    }

    public Optional<Map<String, Object>> findById(String resource, String id) {
        requireResource(resource);
        ColumnDefinition idColumn = findColumn(columns(resource), "id");
        if (idColumn == null)
            throw new IllegalArgumentException("Resource '" + resource + "' không có trường id.");
        String sql = "SELECT * FROM `" + resource + "` WHERE `id` = ? LIMIT 1";
        List<Map<String, Object>> rows = jdbc.query(sql, this::mapRow, toDatabaseValue(idColumn, id));
        return rows.stream().findFirst();
    }

    public Map<String, Object> insert(String resource, Map<String, Object> payload) {
        requireResource(resource);
        List<ColumnDefinition> columns = columns(resource);
        Map<String, Object> body = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        ColumnDefinition idColumn = findColumn(columns, "id");
        if (idColumn != null && (body.get("id") == null || String.valueOf(body.get("id")).isBlank())) {
            body.put("id", nextId(resource));
        }
        if ("settings".equals(resource)) {
            List<Map<String, Object>> current = findAll(resource, Map.of());
            if (!current.isEmpty())
                return update(resource, "settings", body);
        }
        List<String> names = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (ColumnDefinition col : columns) {
            if ("_empty_placeholder".equals(col.name()))
                continue;
            Object value = body.containsKey(col.name()) ? body.get(col.name()) : defaultValue(col);
            if (value == Missing.VALUE)
                continue;
            names.add("`" + col.name() + "`");
            values.add(toDatabaseValue(col, value));
        }
        if (names.isEmpty())
            throw new IllegalArgumentException("Không có trường dữ liệu hợp lệ để lưu.");
        String sql = "INSERT INTO `" + resource + "` (" + String.join(",", names) + ") VALUES ("
                + names.stream().map(n -> "?").collect(Collectors.joining(",")) + ")";
        jdbc.update(sql, values.toArray());
        if ("settings".equals(resource))
            return findAll(resource, Map.of()).stream().findFirst().orElse(body);
        Object id = body.get("id");
        if (id != null && findColumn(columns, "id") != null)
            return findById(resource, String.valueOf(id)).orElse(body);
        return body;
    }

    public Map<String, Object> update(String resource, String id, Map<String, Object> patch) {
        requireResource(resource);
        List<ColumnDefinition> columns = columns(resource);
        if ("settings".equals(resource)) {
            List<Map<String, Object>> current = findAll(resource, Map.of());
            if (current.isEmpty())
                return insert(resource, patch);
            id = "settings";
        } else if (findById(resource, id).isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy " + resource + " với id=" + id);
        }
        List<String> sets = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> entry : (patch == null ? Map.<String, Object>of() : patch).entrySet()) {
            ColumnDefinition col = findColumn(columns, entry.getKey());
            if (col == null || "id".equalsIgnoreCase(col.name()) || "_empty_placeholder".equals(col.name()))
                continue;
            sets.add("`" + col.name() + "` = ?");
            values.add(toDatabaseValue(col, entry.getValue()));
        }
        if (!sets.isEmpty()) {
            String where = "settings".equals(resource) ? "1=1" : "`id` = ?";
            if (!"settings".equals(resource))
                values.add(toDatabaseValue(findColumn(columns, "id"), id));
            jdbc.update("UPDATE `" + resource + "` SET " + String.join(",", sets) + " WHERE " + where,
                    values.toArray());
        }
        if ("settings".equals(resource))
            return findAll(resource, Map.of()).stream().findFirst().orElse(Map.of());
        final String lookupId = id;
        return findById(resource, lookupId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy " + resource + " với id=" + lookupId));
    }

    public void delete(String resource, String id) {
        requireResource(resource);
        ColumnDefinition idColumn = findColumn(columns(resource), "id");
        if (idColumn == null)
            throw new IllegalArgumentException("Resource '" + resource + "' không hỗ trợ xóa theo id.");
        int changed = jdbc.update("DELETE FROM `" + resource + "` WHERE `id` = ?", toDatabaseValue(idColumn, id));
        if (changed == 0)
            throw new ResourceNotFoundException("Không tìm thấy " + resource + " với id=" + id);
    }

    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String name = md.getColumnLabel(i);
            Object value = rs.getObject(i);
            String type = md.getColumnTypeName(i);
            if (value != null && "JSON".equalsIgnoreCase(type)) {
                try {
                    value = mapper.readValue(value.toString(), Object.class);
                } catch (Exception ignored) {
                    /* retain source text if legacy JSON is malformed */ }
            } else if (value != null && "tinyint".equalsIgnoreCase(type)
                    && md.getColumnType(i) == java.sql.Types.TINYINT && value instanceof Number n
                    && md.getColumnDisplaySize(i) == 1) {
                value = n.intValue() != 0;
            }
            row.put(name, value);
        }
        return row;
    }

    private List<ColumnDefinition> columns(String resource) {
        requireResource(resource);
        synchronized (metadataCache) {
            if (metadataCache.containsKey(resource))
                return metadataCache.get(resource);
            List<ColumnDefinition> found = jdbc.query(
                    "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA " +
                            "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                    (rs, n) -> new ColumnDefinition(rs.getString("COLUMN_NAME"), rs.getString("DATA_TYPE"),
                            rs.getString("COLUMN_TYPE"),
                            "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")), rs.getObject("COLUMN_DEFAULT"),
                            rs.getString("EXTRA")),
                    resource);
            if (found.isEmpty())
                throw new IllegalArgumentException("Không tìm thấy bảng `" + resource + "` trong database hiện tại.");
            metadataCache.put(resource, found);
            return found;
        }
    }

    private Object nextId(String resource) {
        if (findColumn(columns(resource), "id") == null)
            return null;
        Long max = jdbc.queryForObject("SELECT COALESCE(MAX(`id`), 0) FROM `" + resource + "`", Long.class);
        return (max == null ? 0L : max) + 1L;
    }

    private Object defaultValue(ColumnDefinition col) {
        if (col.defaultValue() != null)
            return col.defaultValue();
        if (col.nullable())
            return null;
        if (col.isJson())
            return (col.name().equals("extraImages") ? "[]" : "{}");
        if (col.isBoolean())
            return false;
        if (col.isNumeric())
            return "double".equalsIgnoreCase(col.dataType()) || "float".equalsIgnoreCase(col.dataType())
                    || "decimal".equalsIgnoreCase(col.dataType()) ? 0.0 : 0L;
        if ("id".equalsIgnoreCase(col.name()) && col.extra() != null && col.extra().contains("auto_increment"))
            return Missing.VALUE;
        return "";
    }

    private Object toDatabaseValue(ColumnDefinition col, Object value) {
        if (value == null)
            return null;

        // Các cột LONGTEXT có CHECK(JSON_VALID(...)) cũng phải được serialize JSON.
        boolean jsonTextColumn = Set.of("gps", "bluetooth", "qr", "extraImages", "translations")
                .contains(col.name());

        if (col.isJson() || jsonTextColumn) {
            if (value instanceof String s) {
                try {
                    // Nếu đã là JSON hợp lệ thì giữ nguyên, tránh mã hóa hai lần.
                    mapper.readTree(s);
                    return s;
                } catch (Exception ignored) {
                    // Chuỗi thường cần được serialize thành JSON string hợp lệ.
                    try {
                        return mapper.writeValueAsString(s);
                    } catch (JsonProcessingException ex) {
                        throw new IllegalArgumentException(
                                "Không thể chuyển trường " + col.name() + " thành JSON.", ex);
                    }
                }
            }

            try {
                return mapper.writeValueAsString(value);
            } catch (JsonProcessingException ex) {
                throw new IllegalArgumentException(
                        "Không thể chuyển trường " + col.name() + " thành JSON.", ex);
            }
        }

        if (col.isBoolean()) {
            if (value instanceof Boolean b)
                return b ? 1 : 0;
            if (value instanceof Number n)
                return n.intValue() == 0 ? 0 : 1;
            String s = String.valueOf(value).trim();
            return ("true".equalsIgnoreCase(s) || "1".equals(s)
                    || "yes".equalsIgnoreCase(s)) ? 1 : 0;
        }

        if (col.isNumeric()) {
            String s = String.valueOf(value).trim();
            if (s.isEmpty())
                return null;
            try {
                return switch (col.dataType().toLowerCase()) {
                    case "double", "float", "real" -> Double.valueOf(s);
                    case "decimal", "numeric" -> new BigDecimal(s);
                    default -> Long.valueOf(s);
                };
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Trường " + col.name() + " phải là số.");
            }
        }

        return value instanceof Map<?, ?> || value instanceof List<?>
                ? value.toString()
                : String.valueOf(value);
    }

    private ColumnDefinition findColumn(List<ColumnDefinition> columns, String name) {
        if (name == null)
            return null;
        return columns.stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    private void requireResource(String resource) {
        if (!supports(resource))
            throw new IllegalArgumentException("Resource không được hỗ trợ: " + resource);
    }

    private int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private enum Missing {
        VALUE
    }
}
