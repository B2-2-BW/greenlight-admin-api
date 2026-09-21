package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.db.repository.mapper.alert.AlertPolicyMapper;
import com.winten.greenlight.admin.db.repository.redis.alert.AlertPolicyCacheRepository;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertPolicyServiceTest {
    @Mock private AlertPolicyMapper alertPolicyMapper;
    @Mock private AlertPolicyCacheRepository alertPolicyCacheRepository;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getReturnsDefaultsWhenRowMissing() {
        authenticateSuper();
        when(alertPolicyMapper.findBySiteId("site-a")).thenReturn(null);

        AlertPolicy policy = service().get("site-a");

        assertThat(policy.getSiteId()).isEqualTo("site-a");
        assertThat(policy.getWaitingCompare()).isEqualTo(QueueAlertCompare.COUNT);
        assertThat(policy.getWaitingThreshold()).isEqualTo(1d);
        assertThat(policy.getSurgeWaitTimeSeconds()).isEqualTo(60);
    }

    @Test
    void updateWritesSiteRowAndCache() {
        authenticateSuper();
        AlertPolicy saved = AlertPolicy.defaults("site-a");
        saved.setForTicks(3);
        when(alertPolicyMapper.findBySiteId("site-a")).thenReturn(saved);

        AlertPolicy result = service().update("site-a", AlertPolicy.builder()
                .forTicks(3)
                .repeatIntervalSeconds(120)
                .waitingCompare(QueueAlertCompare.COUNT)
                .waitingThreshold(10d)
                .build());

        ArgumentCaptor<AlertPolicy> captor = ArgumentCaptor.forClass(AlertPolicy.class);
        verify(alertPolicyMapper).upsert(captor.capture());
        assertThat(captor.getValue().getSiteId()).isEqualTo("site-a");
        assertThat(captor.getValue().getWaitingThreshold()).isEqualTo(10d);
        assertThat(captor.getValue().getActiveCompare()).isEqualTo(QueueAlertCompare.COUNT);
        verify(alertPolicyCacheRepository).updateAlertPolicy(saved);
        assertThat(result.getForTicks()).isEqualTo(3);
    }

    private AlertPolicyService service() {
        return new AlertPolicyService(alertPolicyMapper, alertPolicyCacheRepository);
    }

    private void authenticateSuper() {
        var user = CurrentUser.builder()
                .accountId(1L)
                .userId("super")
                .userRole(UserRole.SUPER)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }
}
