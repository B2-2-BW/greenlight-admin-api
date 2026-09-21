package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.db.repository.mapper.alert.AlertSubscriptionMapper;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertSubscriptionServiceTest {
    @Mock private AlertSubscriptionMapper alertSubscriptionMapper;
    @Mock private AlertChannelService alertChannelService;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMineFillsCatalogDefaults() {
        authenticate();
        when(alertSubscriptionMapper.findByAccountId(1L)).thenReturn(List.of(
                AlertSubscription.builder().alertname("QUEUE_WAIT").enabled(true).build()
        ));

        List<AlertSubscription> mine = service().getMine();

        assertThat(mine).extracting(AlertSubscription::getAlertname)
                .containsExactly(
                        "QUEUE_WAIT", "QUEUE_DISABLED", "SITE_MAINTENANCE"
                );
        assertThat(mine.get(0).isEnabled()).isTrue();
        assertThat(mine.get(1).isEnabled()).isFalse();
    }

    @Test
    void getMineIgnoresRemovedAlertTypes() {
        authenticate();
        when(alertSubscriptionMapper.findByAccountId(1L)).thenReturn(List.of(
                AlertSubscription.builder().alertname("QUEUE_WAIT").enabled(true).build(),
                AlertSubscription.builder().alertname("ACTIVE_USERS").enabled(true).build(),
                AlertSubscription.builder().alertname("VISITOR_SURGE").enabled(true).build()
        ));

        assertThat(service().getMine()).extracting(AlertSubscription::getAlertname)
                .containsExactly("QUEUE_WAIT", "QUEUE_DISABLED", "SITE_MAINTENANCE")
                .doesNotContain("ACTIVE_USERS", "VISITOR_SURGE");
    }

    @Test
    void getMineTreatsLegacySiteDisabledAsQueueDisabled() {
        authenticate();
        when(alertSubscriptionMapper.findByAccountId(1L)).thenReturn(List.of(
                AlertSubscription.builder().alertname("SITE_DISABLED").enabled(true).build()
        ));

        List<AlertSubscription> mine = service().getMine();
        assertThat(mine).filteredOn(item -> "QUEUE_DISABLED".equals(item.getAlertname()))
                .extracting(AlertSubscription::isEnabled)
                .containsExactly(true);
    }

    @Test
    void getMineTreatsLegacySchedulerSubscriptionsAsInfra() {
        authenticate(UserRole.SITE_ADMIN);
        when(alertSubscriptionMapper.findByAccountId(1L)).thenReturn(List.of(
                AlertSubscription.builder().alertname("SCHEDULER_STOPPED").enabled(true).build()
        ));

        List<AlertSubscription> mine = service().getMine();
        assertThat(mine).extracting(AlertSubscription::getAlertname)
                .containsExactly("QUEUE_WAIT", "QUEUE_DISABLED", "SITE_MAINTENANCE", "INFRA");
        assertThat(mine).filteredOn(item -> "INFRA".equals(item.getAlertname()))
                .extracting(AlertSubscription::isEnabled)
                .containsExactly(true);
    }

    @Test
    void getMineIncludesPlatformAlertsForSiteAdmin() {
        authenticate(UserRole.SITE_ADMIN);
        when(alertSubscriptionMapper.findByAccountId(1L)).thenReturn(List.of());

        assertThat(service().getMine()).extracting(AlertSubscription::getAlertname)
                .containsExactly(
                        "QUEUE_WAIT", "QUEUE_DISABLED", "SITE_MAINTENANCE", "INFRA"
                );
    }

    @Test
    void updateMineRejectsUnknownAlertname() {
        authenticate();
        assertThatThrownBy(() -> service().updateMine(List.of(
                AlertSubscription.builder().alertname("Nope").enabled(true).build()
        ))).isInstanceOf(CoreException.class);
    }

    @Test
    void updateMineRejectsPlatformAlertForUser() {
        authenticate();
        assertThatThrownBy(() -> service().updateMine(List.of(
                AlertSubscription.builder().alertname("INFRA").enabled(true).build()
        ))).isInstanceOf(CoreException.class);
    }

    @Test
    void updateMineUpsertsCurrentUser() {
        authenticate();
        when(alertSubscriptionMapper.findByAccountId(1L)).thenReturn(List.of());

        service().updateMine(List.of(
                AlertSubscription.builder().alertname("QUEUE_WAIT").enabled(true).build()
        ));

        var captor = ArgumentCaptor.forClass(AlertSubscription.class);
        verify(alertSubscriptionMapper).upsert(captor.capture());
        assertThat(captor.getValue().getAccountId()).isEqualTo(1L);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo("user-a");
        assertThat(captor.getValue().isEnabled()).isTrue();
    }

    private AlertSubscriptionService service() {
        return new AlertSubscriptionService(alertSubscriptionMapper, alertChannelService);
    }

    private void authenticate() {
        authenticate(UserRole.USER);
    }

    private void authenticate(UserRole role) {
        var user = CurrentUser.builder()
                .accountId(1L)
                .userId("user-a")
                .userRole(role)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }
}
