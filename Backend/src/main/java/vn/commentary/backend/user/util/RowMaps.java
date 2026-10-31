package vn.commentary.backend.user.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Đọc một dòng ResultSet thành Map (khoá viết thường) và các hàm lấy giá trị an toàn.
 * Dùng để chịu được sự khác nhau về kiểu cột giữa MySQL/MariaDB (LONGTEXT, tinyint(1)...) và H2 khi test.
 */
public final class RowMaps {
    private RowMaps() {
    }

    public static Map<String, Object> toMap(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String label = md.getColumnLabel(i).toLowerCase(Locale.ROOT);
            int type = md.getColumnType(i);
            Object value;
            if (type == Types.BIT || type == Types.BOOLEAN) {
                boolean b = rs.getBoolean(i);
                value = rs.wasNull() ? null : b;
            } else if (type == Types.TINYINT || type == Types.SMALLINT || type == Types.INTEGER
                    || type == Types.BIGINT) {
                long l = rs.getLong(i);
                value = rs.wasNull() ? null : l;
            } else if (type == Types.REAL || type == Types.FLOAT || type == Types.DOUBLE
                    || type == Types.DECIMAL || type == Types.NUMERIC) {
                double d = rs.getDouble(i);
                value = rs.wasNull() ? null : d;
            } else {
                value = rs.getString(i);
            }
            row.put(label, value);
        }
        return row;
    }

    public static String str(Map<String, Object> row, String key) {
        Object v = row.get(key.toLowerCase(Locale.ROOT));
        return v == null ? "" : v.toString();
    }

    public static long lng(Map<String, Object> row, String key) {
        Object v = row.get(key.toLowerCase(Locale.ROOT));
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** Trả về null nếu cột trống hoặc không phải số. */
    public static Double dbl(Map<String, Object> row, String key) {
        Object v = row.get(key.toLowerCase(Locale.ROOT));
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean bool(Map<String, Object> row, String key) {
        Object v = row.get(key.toLowerCase(Locale.ROOT));
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        String s = v.toString().trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
    }
}
