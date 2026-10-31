package vn.commentary.backend.user.service;

import org.springframework.stereotype.Service;
import vn.commentary.backend.user.dto.Dtos.ArtifactSummary;
import vn.commentary.backend.user.dto.Dtos.Catalog;
import vn.commentary.backend.user.dto.Dtos.CatalogNarration;
import vn.commentary.backend.user.dto.Dtos.LanguageDto;
import vn.commentary.backend.user.dto.Dtos.ListenTotal;
import vn.commentary.backend.user.dto.Dtos.SiteSummary;
import vn.commentary.backend.user.model.Models.Artifact;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.model.Models.Narration;
import vn.commentary.backend.user.model.Models.Site;
import vn.commentary.backend.user.model.Models.TargetInfo;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.repository.ListenRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gói dữ liệu khởi tạo cho trang hành khách: một lời gọi thay cho 7 lời gọi riêng lẻ
 * (địa điểm, hiện vật, thuyết minh, ngôn ngữ, lượt nghe, lịch sử, yêu thích).
 */
@Service
public class CatalogService {
    private static final int HISTORY_LIMIT = 100;

    private final ContentService content;
    private final ActivityService activity;
    private final ListenRepository listens;

    public CatalogService(ContentService content, ActivityService activity, ListenRepository listens) {
        this.content = content;
        this.activity = activity;
        this.listens = listens;
    }

    public Catalog build(DeviceSession session) {
        List<LanguageDto> languages = content.enabledLanguages().stream()
                .map(l -> new LanguageDto(l.id(), l.name(), l.code(), l.ttsKey())).toList();

        List<Site> siteList = content.sites(null, null);
        List<Artifact> artifactList = content.artifacts(null, null);

        Map<Long, Integer> counts = new HashMap<>();
        Map<Long, String> siteNames = new HashMap<>();
        for (Site s : siteList) siteNames.put(s.id(), s.name());
        for (Artifact a : artifactList) counts.merge(a.siteId(), 1, Integer::sum);

        List<SiteSummary> sites = siteList.stream().map(s -> new SiteSummary(s.id(), s.name(), s.classify(),
                s.city(), s.address(), s.description(), s.latitude(), s.longitude(),
                imageUrl(TargetType.SITE, s.id(), s.hasImage()), counts.getOrDefault(s.id(), 0))).toList();
        List<ArtifactSummary> artifacts = artifactList.stream().map(a -> new ArtifactSummary(a.id(), a.siteId(),
                siteNames.getOrDefault(a.siteId(), ""), a.name(), a.address(), a.description(), a.latitude(),
                a.longitude(), imageUrl(TargetType.ARTIFACT, a.id(), a.hasImage()))).toList();

        Map<String, TargetInfo> visible = content.visibleTargetIndex();
        List<CatalogNarration> narrations = content.narrations().stream()
                .filter(n -> visible.containsKey(ContentService.key(n.targetType(), n.targetId())))
                .map(CatalogService::toCatalogNarration).toList();

        List<ListenTotal> totals = listens.totalsByTarget().stream()
                .filter(t -> visible.containsKey(t.targetType() + ":" + t.targetId()))
                .map(t -> new ListenTotal(t.targetType(), t.targetId(), t.count())).toList();

        return new Catalog(languages, sites, artifacts, narrations, totals,
                activity.history(session, HISTORY_LIMIT), activity.likes(session));
    }

    private static CatalogNarration toCatalogNarration(Narration n) {
        String audioUrl = n.audioEnabled() && n.hasAudio()
                ? "/api/user/narrations/" + n.targetType().code() + "/" + n.targetId() + "/audio" : null;
        return new CatalogNarration(n.targetType().code(), n.targetId(), n.text() == null ? "" : n.text(),
                n.useDescription(), n.ttsEnabled(), n.speed() > 0 ? n.speed() : 1.0, n.audioEnabled(), audioUrl,
                n.gps(), n.bluetooth(), n.qr());
    }

    private static String imageUrl(TargetType type, long id, boolean hasImage) {
        return hasImage ? "/api/user/" + type.table() + "/" + id + "/image" : null;
    }
}
