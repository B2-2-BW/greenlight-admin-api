package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.db.repository.mapper.alert.AlertChannelTargetMapper;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertChannelServiceTest {
    @Mock private AlertChannelTargetMapper alertChannelTargetMapper;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void normalizeDropsDuplicatesAndBlankRows() {
        List<AlertChannelTarget> normalized = AlertChannelService.normalize(List.of(
                AlertChannelTarget.builder().channel("teams").target("  abc  ").build(),
                AlertChannelTarget.builder().channel("TEAMS").target("abc").build(),
                AlertChannelTarget.builder().channel("EMAIL").target("a@b.c").build(),
                AlertChannelTarget.builder().channel("").target("").build()
        ));
        assertThat(normalized).extracting(AlertChannelTarget::getChannel)
                .containsExactly("TEAMS", "EMAIL");
        assertThat(normalized).extracting(AlertChannelTarget::getTarget)
                .containsExactly("abc", "a@b.c");
    }

    @Test
    void normalizeRejectsUnknownChannel() {
        assertThatThrownBy(() -> AlertChannelService.normalize(List.of(
                AlertChannelTarget.builder().channel("SMS").target("010").build()
        ))).isInstanceOf(CoreException.class);
    }

    @Test
    void replaceMineDeletesThenInserts() {
        authenticate();
        when(alertChannelTargetMapper.findByAccountId(1L)).thenReturn(List.of());

        service().replaceMine(List.of(
                AlertChannelTarget.builder().channel("TEAMS").target("ops-group").build()
        ));

        verify(alertChannelTargetMapper).deleteByAccountId(1L);
        var captor = ArgumentCaptor.forClass(AlertChannelTarget.class);
        verify(alertChannelTargetMapper, times(1)).insert(captor.capture());
        assertThat(captor.getValue().getAccountId()).isEqualTo(1L);
        assertThat(captor.getValue().getChannel()).isEqualTo("TEAMS");
        assertThat(captor.getValue().getTarget()).isEqualTo("ops-group");
    }

    private AlertChannelService service() {
        return new AlertChannelService(alertChannelTargetMapper);
    }

    private void authenticate() {
        var user = CurrentUser.builder()
                .accountId(1L)
                .userId("user-a")
                .userRole(UserRole.USER)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }
}
