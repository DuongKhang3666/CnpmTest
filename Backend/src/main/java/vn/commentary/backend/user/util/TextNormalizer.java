package vn.commentary.backend.user.util;

import java.text.Normalizer;
import java.util.Locale;

/** Chuẩn hoá văn bản để tìm kiếm không phân biệt hoa/thường và dấu tiếng Việt. */
public final class TextNormalizer {
    private TextNormalizer() {
    }

    public static String fold(String value) {
        if (value == null) return "";
        String replaced = value.replace('đ', 'd').replace('Đ', 'D');
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    /** Chuẩn hoá địa chỉ MAC: bỏ dấu ':' '-' khoảng trắng, viết hoa. */
    public static String normalizeMac(String mac) {
        if (mac == null) return "";
        return mac.replaceAll("[:\\-\\s]", "").toUpperCase(Locale.ROOT);
    }
}
