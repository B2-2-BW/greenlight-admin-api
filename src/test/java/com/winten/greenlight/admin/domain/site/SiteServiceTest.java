package com.winten.greenlight.admin.domain.site;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.redis.site.SiteCacheRepository;
import com.winten.greenlight.admin.api.controller.site.SiteResponse;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {
    @Mock private SiteMapper siteMapper;
    @Mock private SiteCacheRepository siteCacheRepository;
    private final SiteApiKeyGenerator siteApiKeyGenerator = new SiteApiKeyGenerator();

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void siteAdminListsOnlyOwnSiteAndCapsPage() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);
        when(siteMapper.countSites("site-a", "alpha", true)).thenReturn(21L);
        when(siteMapper.findSitesPage("site-a", "alpha", true, 10, 20)).thenReturn(List.of());

        var result = service.getManageableSites(99, 10, " alpha ", true);

        assertThat(result.getPage()).isEqualTo(3);
        verify(siteMapper).findSitesPage("site-a", "alpha", true, 10, 20);
    }

    @Test
    void siteAdminCannotManageAnotherSite() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);
        assertThatThrownBy(() -> service.getManageableSite("site-b"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void regularUpdateCannotChangeApiKey() {
        var service = service();
        authenticate("super", "site-a", UserRole.SUPER);
        var request = SiteInfo.builder()
                .siteId("site-a")
                .siteName("사이트 이름")
                .siteApiKey("new-key")
                .build();
        var updated = SiteInfo.builder().siteId("site-a").siteApiKey("old-key").build();
        when(siteMapper.updateSiteInfoById(any())).thenReturn(1);
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(updated));

        service.updateSiteInfoById(request, false);

        var captured = ArgumentCaptor.forClass(SiteInfo.class);
        verify(siteMapper).updateSiteInfoById(captured.capture());
        assertThat(captured.getValue().getSiteApiKey()).isNull();
        verify(siteCacheRepository, never()).updateSiteApiKeyCache(any());
    }

    @Test
    void siteAdminCanUpdateOnlyOwnSiteQueueEnabled() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);
        var updated = SiteInfo.builder()
                .siteId("site-a")
                .siteEnabled(true)
                .queueEnabled(false)
                .build();
        when(siteMapper.updateSiteInfoById(any())).thenReturn(1);
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(updated));

        var result = service.updateQueueEnabled("site-a", false);

        assertThat(result).isSameAs(updated);
        var captured = ArgumentCaptor.forClass(SiteInfo.class);
        verify(siteMapper).updateSiteInfoById(captured.capture());
        assertThat(captured.getValue().getQueueEnabled()).isFalse();
        assertThat(captured.getValue().getSiteEnabled()).isNull();
        verify(siteCacheRepository).updateSiteInfo(updated);
    }

    @Test
    void siteAdminCannotUpdateSiteManagementFieldsEvenForOwnSite() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);
        var request = SiteInfo.builder()
                .siteId("site-a")
                .siteName("변경 이름")
                .queueEnabled(false)
                .build();

        assertThatThrownBy(() -> service.updateSiteInfoById(request, true))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        verifyNoInteractions(siteMapper, siteCacheRepository);
    }

    @Test
    void siteAdminCannotUpdateAnotherSiteQueueEnabled() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);

        assertThatThrownBy(() -> service.updateQueueEnabled("site-b", false))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        verifyNoInteractions(siteMapper, siteCacheRepository);
    }

    @Test
    void regularUserCannotUpdateQueueEnabled() {
        var service = service();
        authenticate("user", "site-a", UserRole.USER);

        assertThatThrownBy(() -> service.updateQueueEnabled("site-a", false))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        verifyNoInteractions(siteMapper, siteCacheRepository);
    }

    @Test
    void explicitNullQueueEnabledIsRejected() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);

        assertThatThrownBy(() -> service.updateQueueEnabled("site-a", null))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.INVALID_DATA);
        verifyNoInteractions(siteMapper, siteCacheRepository);
    }

    @Test
    void superRotatesApiKeyAndRemovesOldCacheKey() {
        var service = service();
        authenticate("super", "site-a", UserRole.SUPER);
        var oldSite = SiteInfo.builder().siteId("site-b").siteApiKey("old-key").siteEnabled(true).build();
        var updatedSite = SiteInfo.builder().siteId("site-b").siteApiKey("gl_new-key").siteEnabled(true).build();
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(oldSite), Optional.of(updatedSite));
        when(siteMapper.updateSiteApiKey(any())).thenReturn(1);

        var key = service.rotateSiteApiKey("site-b");

        var captured = ArgumentCaptor.forClass(SiteInfo.class);
        verify(siteMapper).updateSiteApiKey(captured.capture());
        assertThat(captured.getValue().getSiteId()).isEqualTo("site-b");
        assertThat(captured.getValue().getSiteApiKey()).isEqualTo(key);
        assertThat(key).matches("gl_[A-Za-z0-9_-]{43}");
        verify(siteCacheRepository).deleteSiteApiKeyCache("old-key");
        verify(siteCacheRepository).updateSiteApiKeyCache(updatedSite);
    }

    @Test
    void nonSuperCannotRotateApiKey() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);

        assertThatThrownBy(() -> service.rotateSiteApiKey("site-a"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        verifyNoInteractions(siteMapper, siteCacheRepository);
    }

    @Test
    void generatedApiKeysHaveAtLeast256BitsOfUrlSafeEntropy() {
        String first = siteApiKeyGenerator.generate();
        String second = siteApiKeyGenerator.generate();

        assertThat(first).matches("gl_[A-Za-z0-9_-]{43}");
        assertThat(second).matches("gl_[A-Za-z0-9_-]{43}");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void regularSiteResponseNeverContainsAnApiKey() {
        assertThat(SiteResponse.class.getDeclaredFields())
                .extracting(field -> field.getName())
                .doesNotContain("siteApiKey", "apiKey");
    }

    private SiteService service() {
        return new SiteService(siteMapper, siteCacheRepository, siteApiKeyGenerator);
    }

    private void authenticate(String userId, String siteId, UserRole role) {
        var user = CurrentUser.builder().accountId(1L).userId(userId).userSiteId(siteId).userRole(role).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
