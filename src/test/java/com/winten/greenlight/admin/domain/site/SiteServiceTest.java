package com.winten.greenlight.admin.domain.site;

import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomMapper;
import com.winten.greenlight.admin.db.repository.mapper.user.UserMapper;
import com.winten.greenlight.admin.db.repository.redis.site.SiteCacheRepository;
import com.winten.greenlight.admin.api.controller.site.SiteResponse;
import com.winten.greenlight.admin.domain.audit.AuditAction;
import com.winten.greenlight.admin.domain.audit.AuditService;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    @Mock private AuditService auditService;
    @Mock private RoomMapper roomMapper;
    @Mock private UserMapper userMapper;
    private final SiteApiKeyGenerator siteApiKeyGenerator = new SiteApiKeyGenerator();

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void siteAdminListsOnlyOwnSiteAndCapsPage() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);
        when(siteMapper.countSites(List.of("site-a"), "alpha", true)).thenReturn(21L);
        when(siteMapper.findSitesPage(List.of("site-a"), "alpha", true, 10, 20)).thenReturn(List.of());

        var result = service.getManageableSites(99, 10, " alpha ", true);

        assertThat(result.getPage()).isEqualTo(3);
        verify(siteMapper).findSitesPage(List.of("site-a"), "alpha", true, 10, 20);
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

        service.updateSiteInfoById(request, false, false, "사이트 정보 변경");

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

        var result = service.updateQueueEnabled("site-a", false, "운영 중지");

        assertThat(result).isSameAs(updated);
        var captured = ArgumentCaptor.forClass(SiteInfo.class);
        verify(siteMapper).updateSiteInfoById(captured.capture());
        assertThat(captured.getValue().getQueueEnabled()).isFalse();
        assertThat(captured.getValue().getSiteEnabled()).isNull();
        verify(siteCacheRepository).updateSiteInfo(updated);
    }

    @Test
    void siteAdminCanUpdateOwnSiteNameAndDescription() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);
        var request = SiteInfo.builder()
                .siteId("site-a")
                .siteName("  변경 이름  ")
                .siteDescription("  변경 설명  ")
                .build();
        var previous = SiteInfo.builder()
                .siteId("site-a")
                .siteName("이전 이름")
                .siteDescription("이전 설명")
                .siteEnabled(true)
                .queueEnabled(true)
                .build();
        var updated = SiteInfo.builder()
                .siteId("site-a")
                .siteName("변경 이름")
                .siteDescription("변경 설명")
                .siteEnabled(true)
                .queueEnabled(true)
                .build();
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(previous), Optional.of(updated));
        when(siteMapper.updateSiteInfoById(any())).thenReturn(1);

        var result = service.updateSiteInfoById(request, false, false, "변경");

        assertThat(result).isSameAs(updated);
        var captured = ArgumentCaptor.forClass(SiteInfo.class);
        verify(siteMapper).updateSiteInfoById(captured.capture());
        assertThat(captured.getValue().getSiteName()).isEqualTo("변경 이름");
        assertThat(captured.getValue().getSiteDescription()).isEqualTo("변경 설명");
        assertThat(captured.getValue().getSiteEnabled()).isNull();
    }

    @Test
    void siteAdminCannotUpdateSiteEnabled() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);
        var request = SiteInfo.builder()
                .siteId("site-a")
                .siteEnabled(false)
                .build();

        assertThatThrownBy(() -> service.updateSiteInfoById(request, true, false, "변경"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        verifyNoInteractions(siteMapper, siteCacheRepository);
    }

    @Test
    void siteAdminCannotUpdateAnotherSiteInfo() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);
        var request = SiteInfo.builder()
                .siteId("site-b")
                .siteName("변경 이름")
                .build();

        assertThatThrownBy(() -> service.updateSiteInfoById(request, false, false, "변경"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        verifyNoInteractions(siteMapper, siteCacheRepository);
    }

    @Test
    void disablingSitePreservesRoomAndUserStates() {
        var service = service();
        authenticate("super", "root", UserRole.SUPER);
        var previous = SiteInfo.builder()
                .siteId("site-a")
                .siteEnabled(true)
                .queueEnabled(true)
                .build();
        var updated = SiteInfo.builder()
                .siteId("site-a")
                .siteEnabled(false)
                .queueEnabled(true)
                .build();
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(previous), Optional.of(updated));
        when(siteMapper.updateSiteInfoById(any())).thenReturn(1);

        var result = service.updateSiteInfoById(
                SiteInfo.builder().siteId("site-a").siteEnabled(false).build(),
                true,
                false,
                "운영 중지"
        );

        assertThat(result.getSiteEnabled()).isFalse();
        assertThat(result.getQueueEnabled()).isTrue();
        verifyNoInteractions(roomMapper, userMapper);
    }

    @Test
    void siteAdminCannotUpdateAnotherSiteQueueEnabled() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);

        assertThatThrownBy(() -> service.updateQueueEnabled("site-b", false, "변경"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        verifyNoInteractions(siteMapper, siteCacheRepository);
    }

    @Test
    void regularUserCannotUpdateQueueEnabled() {
        var service = service();
        authenticate("user", "site-a", UserRole.USER);

        assertThatThrownBy(() -> service.updateQueueEnabled("site-a", false, "변경"))
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
        verifyNoInteractions(siteMapper, siteCacheRepository);
    }

    @Test
    void explicitNullQueueEnabledIsRejected() {
        var service = service();
        authenticate("site-admin", "site-a", UserRole.SITE_ADMIN);

        assertThatThrownBy(() -> service.updateQueueEnabled("site-a", null, "변경"))
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

        var key = service.rotateSiteApiKey("site-b", "정기 교체");

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

        assertThatThrownBy(() -> service.rotateSiteApiKey("site-a", "정기 교체"))
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

    @Test
    void superCreatesSiteAndInitializesCachesWithoutAuditingApiKey() {
        var service = service();
        authenticate("super", "root", UserRole.SUPER);
        when(siteMapper.insertSite(any())).thenReturn(1);

        SiteInfo created = service.createSite("new1", "신규 사이트", "설명", "신규 계약");

        assertThat(created.getSiteEnabled()).isTrue();
        assertThat(created.getQueueEnabled()).isFalse();
        assertThat(created.getSiteApiKey()).matches("gl_[A-Za-z0-9_-]{43}");
        verify(siteCacheRepository).updateSiteApiKeyCache(created);
        verify(siteCacheRepository).updateSiteInfo(created);
        verify(siteCacheRepository).updateRoomListCache("new1", List.of());
        verify(auditService).recordChanges(
                eq("new1"), eq("SITE"), eq("new1"), eq(AuditAction.CREATE), eq("신규 계약"),
                anyMap(), anyMap(), eq(List.of("siteName", "siteDescription", "siteEnabled", "queueEnabled"))
        );
    }

    @Test
    void softDeleteKeepsRoomAndUserStateBeforeClearingCaches() {
        var service = service();
        authenticate("super", "root", UserRole.SUPER);
        var previous = SiteInfo.builder()
                .siteId("old1")
                .siteApiKey("old-api-key")
                .siteEnabled(true)
                .queueEnabled(true)
                .build();
        when(siteMapper.findSiteById(any())).thenReturn(Optional.of(previous));
        when(siteMapper.softDeleteSite(any())).thenReturn(1);

        service.deleteSite("old1", "계약 종료");

        InOrder order = inOrder(siteMapper, userMapper, auditService, siteCacheRepository);
        order.verify(siteMapper).softDeleteSite(any());
        order.verify(userMapper).deleteSiteAccessBySiteId("old1");
        order.verify(userMapper).reassignHomeSiteIfMissing("old1");
        order.verify(auditService).recordChanges(
                eq("old1"), eq("SITE"), eq("old1"), eq(AuditAction.DELETE), eq("계약 종료"),
                anyMap(), anyMap(), eq(List.of("siteEnabled", "queueEnabled", "deleted"))
        );
        order.verify(siteCacheRepository).deleteSiteApiKeyCache("old-api-key");
        order.verify(siteCacheRepository).updateSiteInfo(any());
        order.verify(siteCacheRepository).updateRoomListCache("old1", List.of());
        verifyNoInteractions(roomMapper);
    }

    private SiteService service() {
        return new SiteService(
                siteMapper, siteCacheRepository, siteApiKeyGenerator, auditService, userMapper
        );
    }

    private void authenticate(String userId, String siteId, UserRole role) {
        var user = CurrentUser.builder().accountId(1L).userId(userId).userSiteId(siteId).userRole(role).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
