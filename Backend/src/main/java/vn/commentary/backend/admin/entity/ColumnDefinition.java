package vn.commentary.backend.admin.entity;

public record ColumnDefinition(String name, String dataType, String columnType,
                               boolean nullable, Object defaultValue, String extra) {
    public boolean isJson() { return "json".equalsIgnoreCase(dataType); }
    public boolean isBoolean() { return "tinyint".equalsIgnoreCase(dataType) && columnType != null && columnType.contains("(1)"); }
    public boolean isNumeric() {
        return switch (dataType.toLowerCase()) {
            case "bigint", "int", "integer", "smallint", "mediumint", "tinyint", "decimal", "numeric", "double", "float", "real" -> true;
            default -> false;
        };
    }
}
