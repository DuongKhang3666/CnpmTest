package vn.commentary.backend.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.commentary.backend.common.exception.ResourceNotFoundException;
import vn.commentary.backend.user.model.Models.Artifact;
import vn.commentary.backend.user.model.Models.Language;
import vn.commentary.backend.user.model.Models.Site;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.repository.ContentRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ContentServiceTest {
    private ContentRepository repo;
    private ContentService service;

    private static Site site(long id, String name, String city, boolean hidden) {
        return new Site(id, name, "Di tích", city, "địa chỉ " + id, "mô tả " + id, 10.0, 106.0, true,
                List.of("e1", "e2"), hidden, "");
    }

    private static Artifact artifact(long id, long siteId, String name, boolean hidden) {
        return new Artifact(id, siteId, name, "", "mô tả hiện vật", null, null, false, hidden, "");
    }

    @BeforeEach
    void setUp() {
        repo = mock(ContentRepository.class);
        service = new ContentService(repo);
        Site s1 = site(1, "Dinh Độc Lập", "Thành phố Hồ Chí Minh", false);
        Site s2 = site(2, "Bảo tàng Chứng tích", "Thành phố Hồ Chí Minh", false);
        Site hiddenSite = site(3, "Ẩn", "Hà Nội", true);
        when(repo.findPublishedSites()).thenReturn(List.of(s1, s2, hiddenSite));
        when(repo.findPublishedSite(anyLong(), anyBoolean())).thenReturn(Optional.empty());
        when(repo.findPublishedSite(1L, false)).thenReturn(Optional.of(s1));
        when(repo.findPublishedSite(1L, true)).thenReturn(Optional.of(s1));
        when(repo.findPublishedSite(3L, false)).thenReturn(Optional.of(hiddenSite));
        when(repo.findPublishedArtifacts()).thenReturn(List.of(
                artifact(10, 1, "Xe tăng", false), artifact(11, 1, "Ẩn", true), artifact(12, 3, "Của site ẩn", false)));
        when(repo.findPublishedArtifact(anyLong())).thenReturn(Optional.empty());
        when(repo.findPublishedArtifact(10L)).thenReturn(Optional.of(artifact(10, 1, "Xe tăng", false)));
        when(repo.findPublishedArtifact(12L)).thenReturn(Optional.of(artifact(12, 3, "Của site ẩn", false)));
    }

    @Test
    void hiddenSitesAreNeverListed() {
        assertEquals(List.of(1L, 2L), service.sites(null, null).stream().map(Site::id).toList());
    }

    @Test
    void searchIgnoresCaseAndVietnameseAccents() {
        assertEquals(List.of(1L), service.sites("doc lap", null).stream().map(Site::id).toList());
        assertEquals(List.of(2L), service.sites("CHỨNG TÍCH", null).stream().map(Site::id).toList());
        assertTrue(service.sites("khong ton tai", null).isEmpty());
        assertEquals(2, service.sites(null, "ho chi minh").size());
    }

    @Test
    void artifactsRequireVisibleParentSiteAndOwnVisibility() {
        assertEquals(List.of(10L), service.artifacts(null, null).stream().map(Artifact::id).toList());
        assertTrue(service.artifacts(2L, null).isEmpty());
        assertEquals(List.of(10L), service.artifacts(1L, "xe tang").stream().map(Artifact::id).toList());
    }

    @Test
    void detailLookupsRejectHiddenOrMissing() {
        assertEquals("Dinh Độc Lập", service.site(1).name());
        assertThrows(ResourceNotFoundException.class, () -> service.site(3));
        assertThrows(ResourceNotFoundException.class, () -> service.site(99));
        assertEquals(10, service.artifact(10).id());
        assertThrows(ResourceNotFoundException.class, () -> service.artifact(12)); // địa điểm chứa nó đang ẩn
    }

    @Test
    void visibleTargetResolvesArtifactWithSiteName() {
        var info = service.visibleTarget(TargetType.ARTIFACT, 10).orElseThrow();
        assertEquals("Dinh Độc Lập", info.siteName());
        assertTrue(service.visibleTarget(TargetType.ARTIFACT, 12).isEmpty());
        assertTrue(service.visibleTarget(TargetType.SITE, 3).isEmpty());
    }

    @Test
    void visibleTargetIndexContainsOnlyVisibleThings() {
        var index = service.visibleTargetIndex();
        assertEquals(3, index.size());
        assertTrue(index.containsKey("site:1"));
        assertTrue(index.containsKey("site:2"));
        assertTrue(index.containsKey("artifact:10"));
    }

    @Test
    void extraImageIndexIsBoundsChecked() {
        when(repo.findExtraImages(1L)).thenReturn(List.of("a", "b"));
        assertEquals("b", service.extraImage(1, 1));
        assertThrows(ResourceNotFoundException.class, () -> service.extraImage(1, 2));
        assertThrows(ResourceNotFoundException.class, () -> service.extraImage(1, -1));
    }

    @Test
    void onlyEnabledLanguagesAreOffered() {
        when(repo.findLanguages()).thenReturn(List.of(new Language(1, "Tiếng Việt", "vi", "vi-VN", true),
                new Language(2, "English", "en", "en-US", true), new Language(3, "Khác", "xx", "xx", false)));
        assertEquals(2, service.enabledLanguages().size());
        assertEquals("en", service.language("en-GB").orElseThrow().code());
        assertTrue(service.language("xx").isEmpty());
    }
}
