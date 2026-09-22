package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.api.controller.webhook.AlertManagerRequest;
import com.winten.greenlight.admin.db.repository.mapper.alert.AlertLogMapper;
import com.winten.greenlight.admin.db.repository.mapper.alert.AlertSendLogMapper;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {
    @Mock private AlertLogMapper alertLogMapper;
    @Mock private AlertSendLogMapper alertSendLogMapper;
    @Mock private TeamsAlertClient teamsAlertClient;
    @Mock private AlertSubscriptionService alertSubscriptionService;
    @Mock private Environment environment;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firingUpsertsByFingerprint() {
        var service = service();
        AlertManagerRequest.Alert alert = alert("FIRING", AlertCatalog.QUEUE_WAIT.name(), "site-a", "room-a");

        service.apply(List.of(alert), "greenlight-scheduler-local");

        var captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(alertLogMapper).upsertFiring(captor.capture());
        AlertLog row = captor.getValue();
        assertThat(row.getAlertname()).isEqualTo(AlertCatalog.QUEUE_WAIT.name());
        assertThat(row.getStatus()).isEqualTo(AlertStatus.FIRING);
        assertThat(row.getSiteId()).isEqualTo("site-a");
        assertThat(row.getRoomId()).isEqualTo("room-a");
        assertThat(row.getFingerprint()).isEqualTo(AlertFingerprint.of(alert.getLabels()));
        assertThat(row.getCreatedBy()).isEqualTo("greenlight-scheduler-local");
        assertThat(row.getEndedAt()).isNull();
    }

    @Test
    void resolvedUpdatesFiringRow() {
        var service = service();
        when(alertLogMapper.resolve(any())).thenReturn(1);
        AlertManagerRequest.Alert alert = alert("RESOLVED", AlertCatalog.QUEUE_WAIT.name(), "site-a", "room-a");

        service.apply(List.of(alert), "greenlight-scheduler-local");

        var captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(alertLogMapper).resolve(captor.capture());
        assertThat(captor.getValue().getEndedAt()).isNotNull();
    }

    @Test
    void resolvedWithoutFiringRowIsIgnored() {
        var service = service();
        when(alertLogMapper.resolve(any())).thenReturn(0);
        AlertManagerRequest.Alert alert = alert("RESOLVED", AlertCatalog.QUEUE_WAIT.name(), "site-a", "room-a");

        service.apply(List.of(alert), "greenlight-scheduler-local");

        verify(alertLogMapper).resolve(any());
        verifyNoInteractions(teamsAlertClient);
    }

    @Test
    void rejectsUnknownStatus() {
        var service = service();
        AlertManagerRequest.Alert alert = alert("pending", AlertCatalog.QUEUE_WAIT.name(), "site-a", "room-a");

        assertThatThrownBy(() -> service.apply(List.of(alert), "scheduler"))
                .isInstanceOf(CoreException.class);
        verifyNoInteractions(alertLogMapper);
    }

    @Test
    void listRequiresAdminAndValidRange() {
        var service = service();
        authenticate("user", "site-a", UserRole.USER);
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();
        assertThatThrownBy(() -> service.getAlertLogs(1, 10, null, null, from, to))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void profileTagUsesFirstActiveProfile() {
        assertThat(AlertService.profileTag(new String[]{"live"})).isEqualTo("[live]");
        assertThat(AlertService.profileTag(new String[]{"dev", "debug"})).isEqualTo("[dev]");
        assertThat(AlertService.profileTag(new String[]{})).isEqualTo("[default]");
    }

    @Test
    void teamsContentUsesSummaryAsTitle() {
        String content = AlertService.teamsContent(
                "[dev]",
                "SCHEDULER_FAILED",
                "FIRING",
                "CRITICAL",
                "[WAITING_TO_READY] 스케쥴러 실행 실패",
                "스케쥴러 실행 연속 4회 실패",
                "2026-09-21T06:12:03Z"
        );
        assertThat(content).isEqualTo(
                "<b>[Greenlight][dev] [WAITING_TO_READY] 스케쥴러 실행 실패</b>"
                        + "<br>[심각] 스케쥴러 실행 연속 4회 실패"
                        + "<br>[At: 2026-09-21 15:12:03]"
        );
    }

    @Test
    void teamsContentMarksResolvedAndCritical() {
        assertThat(AlertService.severityLabel("RESOLVED", "CRITICAL")).isEqualTo("해제");
        assertThat(AlertService.severityLabel("FIRING", "CRITICAL")).isEqualTo("심각");
        assertThat(AlertService.alertTitle("QUEUE_DISABLED")).isEqualTo("사이트 대기열 비활성화");
        assertThat(AlertService.alertTitle("SITE_DISABLED")).isEqualTo("사이트 대기열 비활성화");
        assertThat(AlertService.formatDisplayTime("2026-09-21T06:12:03.123456789Z")).isEqualTo("2026-09-21 15:12:03");
    }

    private AlertService service() {
        return new AlertService(
                alertLogMapper, alertSendLogMapper, teamsAlertClient, alertSubscriptionService, JsonMapper.builder().build(), environment
        );
    }

    private AlertManagerRequest.Alert alert(String status, String alertname, String siteId, String roomId) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("alertname", alertname);
        labels.put("site_id", siteId);
        labels.put("room_id", roomId);
        AlertManagerRequest.Alert alert = new AlertManagerRequest.Alert();
        alert.setStatus(status);
        alert.setLabels(labels);
        alert.setAnnotations(Map.of("summary", alertname));
        alert.setStartsAt(InstantNowIso());
        return alert;
    }

    private String InstantNowIso() {
        return java.time.Instant.now().toString();
    }

    private void authenticate(String userId, String siteId, UserRole role) {
        var user = CurrentUser.builder().accountId(1L).userId(userId).userSiteId(siteId).userRole(role).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }
}
