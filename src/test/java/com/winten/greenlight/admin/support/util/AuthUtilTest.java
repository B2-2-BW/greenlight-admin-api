package com.winten.greenlight.admin.support.util;

import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthUtilTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void regularUserCannotUseSharedUpdateOrDeletePermissionsForOwnSite() {
        authenticate(UserRole.USER, "site-a");

        assertForbidden(() -> AuthUtil.ensureCanUpdate("site-a"));
        assertForbidden(() -> AuthUtil.ensureCanDelete("site-a"));
    }

    @Test
    void siteAdminCanUseSharedUpdateAndDeletePermissionsForOwnSite() {
        authenticate(UserRole.SITE_ADMIN, "site-a");

        AuthUtil.ensureCanUpdate("site-a");
        AuthUtil.ensureCanDelete("site-a");
    }

    private void authenticate(UserRole role, String siteId) {
        var user = CurrentUser.builder()
                .accountId(1L)
                .userId("current-user")
                .userSiteId(siteId)
                .userRole(role)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }

    private void assertForbidden(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CoreException.class)
                .extracting(error -> ((CoreException) error).getErrorType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }
}
