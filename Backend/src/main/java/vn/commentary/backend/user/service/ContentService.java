package vn.commentary.backend.user.service;

import org.springframework.stereotype.Service;
import vn.commentary.backend.exception.ResourceNotFoundException;
import vn.commentary.backend.user.model.Models.Artifact;
import vn.commentary.backend.user.model.Models.Language;
import vn.commentary.backend.user.model.Models.Narration;
import vn.commentary.backend.user.model.Models.Site;
import vn.commentary.backend.user.model.Models.TargetInfo;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.repository.ContentRepository;
import vn.commentary.backend.user.util.TextNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Luật hiển thị nội dung cho người dùng: chỉ nội dung đã xuất bản và không bị ẩn.
 * Hiện vật chỉ hiển thị khi địa điểm chứa nó cũng đang hiển thị.
 */
@Service
public class ContentService {
    private final ContentRepository repository;

    public ContentService(ContentRepository repository) {
        this.repository = repository;
    }

    public List<Language> enabledLanguages() {
        return repository.findLanguages().stream().filter(Language::enabled).toList();
    }

    public Optional<Language> language(String code) {
        if (code == null) return Optional.empty();
        String base = base(code);
        return enabledLanguages().stream()
                .filter(l -> l.code().equalsIgnoreCase(code.trim()) || base(l.code()).equals(base))
                .findFirst();
    }

    /* ---------- Địa điểm ---------- */
    public List<Site> sites(String q, String city) {
        String needle = TextNormalizer.fold(q);
        String cityNeedle = TextNormalizer.fold(city);
        return repository.findPublishedSites().stream()
                .filter(s -> !s.hidden())
                .filter(s -> needle.isEmpty() || TextNormalizer.fold(String.join(" ", s.name(), s.classify(),
                        s.city(), s.address(), s.description())).contains(needle))
                .filter(s -> cityNeedle.isEmpty() || TextNormalizer.fold(s.city()).contains(cityNeedle))
                .toList();
    }

    public Site site(long id) {
        return visibleSite(id, true).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm id=" + id));
    }

    /* ---------- Hiện vật ---------- */
    public List<Artifact> artifacts(Long siteId, String q) {
        Map<Long, Site> visible = visibleSitesById();
        String needle = TextNormalizer.fold(q);
        return repository.findPublishedArtifacts().stream()
                .filter(a -> !a.hidden() && visible.containsKey(a.siteId()))
                .filter(a -> siteId == null || a.siteId() == siteId)
                .filter(a -> needle.isEmpty() || TextNormalizer.fold(String.join(" ", a.name(), a.address(),
                        a.description(), visible.get(a.siteId()).name())).contains(needle))
                .toList();
    }

    public Artifact artifact(long id) {
        return visibleArtifact(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hiện vật id=" + id));
    }

    public String siteName(long siteId) {
        return visibleSite(siteId, false).map(Site::name).orElse("");
    }

    /* ---------- Ảnh (công khai nhưng chỉ cho nội dung đang hiển thị) ---------- */
    public String image(TargetType type, long id) {
        requireVisible(type, id);
        return repository.findImage(type, id).orElseThrow(() -> new ResourceNotFoundException("Chưa có ảnh."));
    }

    public String extraImage(long siteId, int index) {
        requireVisible(TargetType.SITE, siteId);
        List<String> images = repository.findExtraImages(siteId);
        if (index < 0 || index >= images.size()) throw new ResourceNotFoundException("Không có ảnh số " + index + ".");
        return images.get(index);
    }

    /* ---------- Tra cứu đối tượng đang hiển thị ---------- */
    public Optional<TargetInfo> visibleTarget(TargetType type, long id) {
        if (type == TargetType.SITE) {
            return visibleSite(id, false).map(s -> new TargetInfo(TargetType.SITE, s.id(), s.name(),
                    s.description(), null, null));
        }
        return visibleArtifact(id).flatMap(a -> visibleSite(a.siteId(), false).map(s ->
                new TargetInfo(TargetType.ARTIFACT, a.id(), a.name(), a.description(), s.id(), s.name())));
    }

    /** Toàn bộ đối tượng đang hiển thị, khoá dạng {@code "site:1"}. Dùng cho nhận diện GPS/QR/Beacon. */
    public Map<String, TargetInfo> visibleTargetIndex() {
        Map<String, TargetInfo> index = new HashMap<>();
        Map<Long, Site> sites = visibleSitesById();
        for (Site s : sites.values()) {
            index.put(key(TargetType.SITE, s.id()),
                    new TargetInfo(TargetType.SITE, s.id(), s.name(), s.description(), null, null));
        }
        for (Artifact a : repository.findPublishedArtifacts()) {
            Site parent = sites.get(a.siteId());
            if (!a.hidden() && parent != null) {
                index.put(key(TargetType.ARTIFACT, a.id()), new TargetInfo(TargetType.ARTIFACT, a.id(), a.name(),
                        a.description(), parent.id(), parent.name()));
            }
        }
        return index;
    }

    public static String key(TargetType type, long id) {
        return type.code() + ":" + id;
    }

    /* ---------- Thuyết minh ---------- */
    public Optional<Narration> narration(TargetType type, long targetId) {
        return repository.findNarration(type, targetId);
    }

    public List<Narration> narrations() {
        return repository.findAllNarrations();
    }

    public String audio(TargetType type, long targetId) {
        requireVisible(type, targetId);
        Narration n = narration(type, targetId).orElseThrow(() -> new ResourceNotFoundException("Chưa có thuyết minh."));
        if (!n.audioEnabled() || !n.hasAudio()) throw new ResourceNotFoundException("Chưa có tệp âm thanh.");
        return repository.findAudio(type, targetId).map(a -> a.data())
                .filter(d -> d != null && !d.isBlank())
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có tệp âm thanh."));
    }

    /* ---------- nội bộ ---------- */
    public void requireVisible(TargetType type, long id) {
        if (visibleTarget(type, id).isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy " + (type == TargetType.SITE ? "địa điểm" : "hiện vật")
                    + " id=" + id);
        }
    }

    private Optional<Site> visibleSite(long id, boolean withExtra) {
        return repository.findPublishedSite(id, withExtra).filter(s -> !s.hidden());
    }

    private Optional<Artifact> visibleArtifact(long id) {
        return repository.findPublishedArtifact(id).filter(a -> !a.hidden())
                .filter(a -> visibleSite(a.siteId(), false).isPresent());
    }

    private Map<Long, Site> visibleSitesById() {
        Map<Long, Site> map = new HashMap<>();
        for (Site s : new ArrayList<>(repository.findPublishedSites())) {
            if (!s.hidden()) map.put(s.id(), s);
        }
        return map;
    }

    private static String base(String code) {
        String lower = code.trim().toLowerCase(Locale.ROOT);
        int dash = lower.indexOf('-');
        return dash > 0 ? lower.substring(0, dash) : lower;
    }
}
