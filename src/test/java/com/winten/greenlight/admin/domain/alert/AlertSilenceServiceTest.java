package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.db.repository.mapper.alert.AlertSilenceMapper;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertSilenceServiceTest {
    @Mock private AlertSilenceMapper alertSilenceMapper;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRequiresSuperAndStoresOneSystemWindow() {
        authenticate(UserRole.SUPER);
        LocalDateTime startsAt = LocalDateTime.now(AlertSilenceService.ZONE_ID).plusMinutes(5);
        LocalDateTime endsAt = startsAt.plusHours(1);

        AlertSilence created = service().create("queue", startsAt, endsAt, "  배포  ");

        var captor = ArgumentCaptor.forClass(AlertSilence.class);
        verify(alertSilenceMapper).insert(captor.capture());
        assertThat(captor.getValue().getSystemCode()).isEqualTo("QUEUE");
        assertThat(captor.getValue().getStartsAt()).isEqualTo(startsAt);
        assertThat(captor.getValue().getEndsAt()).isEqualTo(endsAt);
        assertThat(captor.getValue().getComment()).isEqualTo("배포");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("super");
        assertThat(created).isSameAs(captor.getValue());
    }

    @Test
    void siteAdminCannotCreate() {
        authenticate(UserRole.SITE_ADMIN);

        assertThatThrownBy(() -> service().create(
                "QUEUE",
                LocalDateTime.now().plusMinutes(1),
                LocalDateTime.now().plusHours(1),
                null
        )).isInstanceOf(CoreException.class);
        verifyNoInteractions(alertSilenceMapper);
    }

    @Test
    void createRejectsUnknownSystemAndEndedWindow() {
        authenticate(UserRole.SUPER);
        LocalDateTime startsAt = LocalDateTime.now(AlertSilenceService.ZONE_ID).minusHours(2);
        LocalDateTime endsAt = LocalDateTime.now(AlertSilenceService.ZONE_ID).minusMinutes(1);

        assertThatThrownBy(() -> service().create("MAIL", startsAt.plusHours(3), endsAt.plusHours(3), null))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_DATA);
        assertThatThrownBy(() -> service().create("REDIS", startsAt, endsAt, null))
                .isInstanceOf(CoreException.class);
        verifyNoInteractions(alertSilenceMapper);
    }

    @Test
    void listActivePassesNormalizedSystem() {
        authenticate(UserRole.SUPER);
        when(alertSilenceMapper.findOpen(eq("DOCKER"), eq(true), any())).thenReturn(List.of());

        service().list("docker", true);

        verify(alertSilenceMapper).findOpen(eq("DOCKER"), eq(true), any());
    }

    @Test
    void cancelSetsCancelledAt() {
        authenticate(UserRole.SUPER);
        AlertSilence existing = AlertSilence.builder().silenceId(4L).systemCode("HOST").build();
        when(alertSilenceMapper.findById(4L)).thenReturn(existing);
        when(alertSilenceMapper.cancel(eq(4L), any(), eq("super"), any(), any())).thenReturn(1);

        AlertSilence cancelled = service().cancel(4L);

        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getUpdatedBy()).isEqualTo("super");
    }

    @Test
    void cancelOfMissingSilenceIsNotFound() {
        authenticate(UserRole.SUPER);
        when(alertSilenceMapper.findById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service().cancel(9L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.SILENCE_NOT_FOUND);
        verify(alertSilenceMapper, never()).cancel(anyLong(), any(), any(), any(), any());
    }

    @Test
    void cancelIsIdempotentWhenAlreadyCancelled() {
        authenticate(UserRole.SUPER);
        LocalDateTime cancelledAt = LocalDateTime.now().minusMinutes(5);
        AlertSilence existing = AlertSilence.builder().silenceId(4L).cancelledAt(cancelledAt).build();
        when(alertSilenceMapper.findById(4L)).thenReturn(existing);

        AlertSilence cancelled = service().cancel(4L);

        assertThat(cancelled.getCancelledAt()).isEqualTo(cancelledAt);
        verify(alertSilenceMapper, never()).cancel(anyLong(), any(), any(), any(), any());
    }

    @Test
    void activeSystemsIgnoresUnknownCodes() {
        when(alertSilenceMapper.findActiveSystemCodes(any())).thenReturn(List.of("QUEUE", "NOPE"));

        assertThat(service().activeSystems(LocalDateTime.now()))
                .containsExactly(AlertSilenceSystem.QUEUE);
    }

    @Test
    void listWithoutSystemDoesNotFilterCode() {
        authenticate(UserRole.SUPER);
        when(alertSilenceMapper.findOpen(isNull(), anyBoolean(), any())).thenReturn(List.of());

        service().list("  ", false);

        verify(alertSilenceMapper).findOpen(isNull(), eq(false), any());
    }

    private AlertSilenceService service() {
        return new AlertSilenceService(alertSilenceMapper);
    }

    private void authenticate(UserRole role) {
        var user = CurrentUser.builder()
                .accountId(1L)
                .userId("super")
                .userRole(role)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }
}
