package vn.commentary.backend.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.commentary.backend.exception.ResourceNotFoundException;
import vn.commentary.backend.user.dto.Dtos.HistoryItem;
import vn.commentary.backend.user.dto.Dtos.HistoryRequest;
import vn.commentary.backend.user.dto.Dtos.LikeItem;
import vn.commentary.backend.user.dto.Dtos.LikeRequest;
import vn.commentary.backend.user.dto.Dtos.ListenRequest;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.model.Models.TargetInfo;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.repository.ListenRepository;
import vn.commentary.backend.user.repository.UserHistoryRepository;
import vn.commentary.backend.user.repository.UserLikeRepository;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Ghi nhận lượt nghe, lịch sử nghe và yêu thích của người dùng (UC8). */
@Service
public class ActivityService {
    private static final Set<String> METHODS = Set.of("manual", "qr", "gps", "beacon", "auto");
    private static final int MAX_HISTORY = 200;

    private final ContentService content;
    private final ListenRepository listens;
    private final UserHistoryRepository history;
    private final UserLikeRepository likes;
    private final Clock clock;
    private final ZoneId zone;

    public ActivityService(ContentService content, ListenRepository listens, UserHistoryRepository history,
                           UserLikeRepository likes, Clock clock,
                           @org.springframework.beans.factory.annotation.Value("${user.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.content = content;
        this.listens = listens;
        this.history = history;
        this.likes = likes;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    /** Cộng 1 lượt nghe cho đối tượng trong ngày (dữ liệu dùng cho báo cáo của admin). */
    @Transactional
    public void recordListen(ListenRequest request) {
        TargetInfo target = requireTarget(request.targetType(), request.targetId());
        String date = clock.instant().atZone(zone).toLocalDate().toString();
        listens.increment(date, target.type().code(), target.id(), language(request.language()));
    }

    @Transactional
    public void addHistory(DeviceSession session, HistoryRequest request) {
        TargetInfo target = requireTarget(request.targetType(), request.targetId());
        String method = normalizeMethod(request.method());
        history.add(session.deviceId(), target.type().code(), target.id(), target.name(),
                language(request.language()), method, clock.millis());
    }

    /** Gom các nhãn do client gửi ("QR code", "GPS", "Bluetooth", "Hàng đợi"...) về tập giá trị cố định. */
    static String normalizeMethod(String raw) {
        String m = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (METHODS.contains(m)) return m;
        if (m.contains("qr")) return "qr";
        if (m.contains("gps")) return "gps";
        if (m.contains("bluetooth") || m.contains("beacon")) return "beacon";
        if (m.contains("hàng đợi") || m.contains("queue")) return "auto";
        return "manual";
    }

    public List<HistoryItem> history(DeviceSession session, Integer limit) {
        int size = limit == null ? 50 : Math.min(MAX_HISTORY, Math.max(1, limit));
        return history.list(session.deviceId(), size);
    }

    public int clearHistory(DeviceSession session) {
        return history.clear(session.deviceId());
    }

    public List<LikeItem> likes(DeviceSession session) {
        return likes.list(session.deviceId());
    }

    @Transactional
    public void like(DeviceSession session, LikeRequest request) {
        TargetInfo target = requireTarget(request.targetType(), request.targetId());
        likes.add(session.deviceId(), target.type().code(), target.id(), target.name(), clock.millis());
    }

    public void unlike(DeviceSession session, String targetType, long targetId) {
        TargetType type = TargetType.from(targetType);
        if (likes.remove(session.deviceId(), type.code(), targetId) == 0) {
            throw new ResourceNotFoundException("Mục này chưa được yêu thích.");
        }
    }

    private TargetInfo requireTarget(String targetType, Long targetId) {
        if (targetId == null) throw new IllegalArgumentException("Thiếu targetId.");
        TargetType type = TargetType.from(targetType);
        return content.visibleTarget(type, targetId).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy đối tượng " + type.code() + " id=" + targetId));
    }

    private static String language(String value) {
        if (value == null || value.isBlank()) return "vi";
        String v = value.trim();
        if (!TranslationService.isValidLanguage(v)) throw new IllegalArgumentException("Mã ngôn ngữ không hợp lệ.");
        return v;
    }
}
