package vn.commentary.backend.user.translation;

/** Cổng dịch thuật. Có thể thay LibreTranslate bằng dịch vụ khác mà không đổi tầng service. */
public interface Translator {
    /**
     * @param source mã ngôn ngữ nguồn hoặc {@code null}/"auto" để tự nhận diện
     * @param target mã ngôn ngữ đích (ví dụ en, ja, zh-CN)
     */
    String translate(String text, String source, String target) throws TranslationException;
}
