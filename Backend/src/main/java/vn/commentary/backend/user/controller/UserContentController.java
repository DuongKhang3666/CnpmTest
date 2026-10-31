package vn.commentary.backend.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.commentary.backend.user.dto.Dtos.ArtifactSummary;
import vn.commentary.backend.user.dto.Dtos.LanguageDto;
import vn.commentary.backend.user.dto.Dtos.SiteDetail;
import vn.commentary.backend.user.dto.Dtos.SiteSummary;
import vn.commentary.backend.user.model.Models.Artifact;
import vn.commentary.backend.user.model.Models.Site;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.service.ContentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Nội dung công khai: ngôn ngữ, địa điểm, hiện vật và ảnh (UC7.2, UC7.3). */
@RestController
@RequestMapping("/api/user")
public class UserContentController {
    private final ContentService content;

    public UserContentController(ContentService content) {
        this.content = content;
    }

    @GetMapping("/languages")
    public List<LanguageDto> languages() {
        return content.enabledLanguages().stream()
                .map(l -> new LanguageDto(l.id(), l.name(), l.code(), l.ttsKey())).toList();
    }

    @GetMapping("/sites")
    public List<SiteSummary> sites(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) String city) {
        List<Site> sites = content.sites(q, city);
        Map<Long, Integer> counts = new HashMap<>();
        for (Artifact a : content.artifacts(null, null)) counts.merge(a.siteId(), 1, Integer::sum);
        return sites.stream().map(s -> new SiteSummary(s.id(), s.name(), s.classify(), s.city(), s.address(),
                s.description(), s.latitude(), s.longitude(), imageUrl(TargetType.SITE, s.id(), s.hasImage()),
                counts.getOrDefault(s.id(), 0))).toList();
    }

    @GetMapping("/sites/{id}")
    public SiteDetail site(@PathVariable long id) {
        Site s = content.site(id);
        List<ArtifactSummary> artifacts = content.artifacts(id, null).stream()
                .map(a -> artifactSummary(a, s.name())).toList();
        List<String> extra = IntStream.range(0, s.extraImages().size())
                .mapToObj(i -> "/api/user/sites/" + id + "/images/" + i).toList();
        return new SiteDetail(s.id(), s.name(), s.classify(), s.city(), s.address(), s.description(),
                s.latitude(), s.longitude(), imageUrl(TargetType.SITE, s.id(), s.hasImage()), extra, artifacts);
    }

    @GetMapping("/artifacts")
    public List<ArtifactSummary> artifacts(@RequestParam(required = false) Long siteId,
                                           @RequestParam(required = false) String q) {
        Map<Long, String> names = new HashMap<>();
        for (Site s : content.sites(null, null)) names.put(s.id(), s.name());
        return content.artifacts(siteId, q).stream()
                .map(a -> artifactSummary(a, names.getOrDefault(a.siteId(), ""))).toList();
    }

    @GetMapping("/artifacts/{id}")
    public ArtifactSummary artifact(@PathVariable long id) {
        Artifact a = content.artifact(id);
        return artifactSummary(a, content.siteName(a.siteId()));
    }

    /* ---------- Ảnh: công khai để nhúng được vào thẻ <img> ---------- */
    @GetMapping("/sites/{id}/image")
    public ResponseEntity<byte[]> siteImage(@PathVariable long id) {
        return MediaResponses.of(content.image(TargetType.SITE, id));
    }

    @GetMapping("/sites/{id}/images/{index}")
    public ResponseEntity<byte[]> siteExtraImage(@PathVariable long id, @PathVariable int index) {
        return MediaResponses.of(content.extraImage(id, index));
    }

    @GetMapping("/artifacts/{id}/image")
    public ResponseEntity<byte[]> artifactImage(@PathVariable long id) {
        return MediaResponses.of(content.image(TargetType.ARTIFACT, id));
    }

    private static ArtifactSummary artifactSummary(Artifact a, String siteName) {
        return new ArtifactSummary(a.id(), a.siteId(), siteName, a.name(), a.address(), a.description(),
                a.latitude(), a.longitude(), imageUrl(TargetType.ARTIFACT, a.id(), a.hasImage()));
    }

    private static String imageUrl(TargetType type, long id, boolean hasImage) {
        return hasImage ? "/api/user/" + type.table() + "/" + id + "/image" : null;
    }
}
