package vn.commentary.backend.user.util;

import java.util.ArrayList;
import java.util.List;

/** Chia văn bản dài thành các đoạn không vượt quá {@code max} ký tự (ưu tiên cắt theo dòng/khoảng trắng). */
public final class TextChunker {
    private TextChunker() {
    }

    public static List<String> split(String text, int max) {
        if (max < 1) throw new IllegalArgumentException("max phải >= 1");
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        StringBuilder cur = new StringBuilder();
        for (String raw : text.split("\n", -1)) {
            String line = raw;
            while (line.length() > max) {
                int cut = line.lastIndexOf(' ', max);
                if (cut <= 0) cut = max;
                append(out, cur, line.substring(0, cut), max);
                line = line.substring(cut).stripLeading();
            }
            append(out, cur, line, max);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private static void append(List<String> out, StringBuilder cur, String piece, int max) {
        if (cur.length() > 0 && cur.length() + 1 + piece.length() > max) {
            out.add(cur.toString());
            cur.setLength(0);
        }
        if (cur.length() > 0) cur.append('\n');
        cur.append(piece);
    }
}
