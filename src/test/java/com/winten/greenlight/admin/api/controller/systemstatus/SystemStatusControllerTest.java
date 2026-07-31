package com.winten.greenlight.admin.api.controller.systemstatus;

import com.winten.greenlight.admin.domain.systemstatus.SystemStatusService;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemStatusControllerTest {
    private final SystemStatusService systemStatusService = mock(SystemStatusService.class);
    private final SystemStatusController controller = new SystemStatusController(systemStatusService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void superCanReadSystemStatus() {
        authenticate(UserRole.SUPER);
        var expected = SystemStatusResponse.builder().build();
        when(systemStatusService.getSystemStatus()).thenReturn(expected);

        var response = controller.getSystemStatus();

        assertThat(response.getBody()).isSameAs(expected);
        verify(systemStatusService).getSystemStatus();
    }

    @Test
    void siteAdminCannotReadSystemStatus() {
        authenticate(UserRole.SITE_ADMIN);

        assertThatThrownBy(controller::getSystemStatus)
                .isInstanceOf(CoreException.class);
    }

    private void authenticate(UserRole role) {
        var user = CurrentUser.builder()
                .userId("tester")
                .userSiteId("site-a")
                .userRole(role)
                .build();
        var authentication = new UsernamePasswordAuthenticationToken(user, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
