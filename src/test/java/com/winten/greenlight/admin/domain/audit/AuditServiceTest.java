package com.winten.greenlight.admin.domain.audit;

import com.winten.greenlight.admin.db.repository.mapper.audit.AuditLogMapper;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {
    @Mock private AuditLogMapper auditLogMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void supportsOnlyCreateUpdateAndDeleteActions() {
        assertThat(AuditAction.values())
                .containsExactly(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.DELETE);
    }

    @Test
    void recordsOnlyChangedAllowedFields() throws Exception {
        var service = new AuditService(auditLogMapper, JsonMapper.builder().build());
        when(auditLogMapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        boolean recorded = service.recordChanges(
                "site-a", "SITE", "site-a", AuditAction.UPDATE, " 운영 중지 ",
                Map.of("queueEnabled", true, "siteApiKey", "secret-old"),
                Map.of("queueEnabled", false, "siteApiKey", "secret-new"),
                List.of("queueEnabled")
        );

        var captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(recorded).isTrue();
        assertThat(auditLog.getReason()).isEqualTo("운영 중지");
        assertThat(auditLog.getChangeDetail()).contains("\"queueEnabled\"").doesNotContain("siteApiKey", "secret");
        Map<?, ?> delta = JsonMapper.builder().build().readValue(auditLog.getChangeDetail(), Map.class);
        assertThat(delta.keySet().stream().map(String::valueOf).toList()).containsExactly("queueEnabled");
    }

    @Test
    void storesMissingReasonAsEmptyString() {
        var service = new AuditService(auditLogMapper, JsonMapper.builder().build());
        when(auditLogMapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        service.recordChanges(
                "site-a", "ROOM", "room-a", AuditAction.UPDATE, null,
                Map.of("enabled", true),
                Map.of("enabled", false),
                List.of("enabled")
        );

        var captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getReason()).isEmpty();
    }

    @Test
    void siteAdminQueryIsForcedToOwnSite() {
        authenticate("admin-a", "site-a", UserRole.SITE_ADMIN);
        var service = new AuditService(auditLogMapper, JsonMapper.builder().build());
        when(auditLogMapper.count("site-a", null, null, null, null, null, null)).thenReturn(0L);

        service.getAuditLogs(1, 20, "site-b", null, null, null, null, null, null);

        verify(auditLogMapper).count("site-a", null, null, null, null, null, null);
    }

    @Test
    void crudActionFilterIsForwardedAsExactDatabaseValue() {
        authenticate("super", "site-a", UserRole.SUPER);
        var service = new AuditService(auditLogMapper, JsonMapper.builder().build());
        when(auditLogMapper.count(null, null, null, null, "UPDATE", null, null)).thenReturn(0L);

        service.getAuditLogs(1, 20, null, null, null, null, AuditAction.UPDATE, null, null);

        verify(auditLogMapper).count(null, null, null, null, "UPDATE", null, null);
    }

    @Test
    void storesCrudActionAndSourcePath() {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/users/pending-user/approval");
        request.setAttribute(
                RequestScopeUtil.SOURCE_PATH_ATTRIBUTE,
                "/users/pending-user"
        );
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var service = new AuditService(auditLogMapper, JsonMapper.builder().build());
        when(auditLogMapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        service.recordChanges(
                "site-a", "USER", "pending-user", AuditAction.UPDATE, "승인",
                Map.of("accountStatus", "PENDING"),
                Map.of("accountStatus", "ACTIVE"),
                List.of("accountStatus")
        );

        var captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("UPDATE");
        assertThat(captor.getValue().getSourcePath()).isEqualTo("/users/pending-user");
    }

    @Test
    void rejectsMissingActionBeforeInsert() {
        var service = new AuditService(auditLogMapper, JsonMapper.builder().build());

        assertThatThrownBy(() -> service.recordChanges(
                "site-a", "SITE", "site-a", null, "수정",
                Map.of("siteName", "이전"),
                Map.of("siteName", "이후"),
                List.of("siteName")
        ))
                .isInstanceOf(com.winten.greenlight.admin.support.error.CoreException.class);
    }

    private void authenticate(String userId, String siteId, UserRole role) {
        var user = CurrentUser.builder()
                .accountId(1L)
                .userId(userId)
                .userSiteId(siteId)
                .userRole(role)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }
}
