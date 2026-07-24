package com.winten.greenlight.admin.support.security;

import com.winten.greenlight.admin.domain.user.AccountStatus;
import com.winten.greenlight.admin.domain.user.CachedUserService;
import com.winten.greenlight.admin.domain.user.User;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final JsonMapper jsonMapper = mock(JsonMapper.class);
    private final CachedUserService cachedUserService = mock(CachedUserService.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtUtil, jsonMapper, cachedUserService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resetRequiredUserCannotAccessOtherApi() throws Exception {
        prepareResetRequiredUser();
        when(jsonMapper.writeValueAsString(any())).thenReturn("{}");
        var request = authenticatedRequest("GET", "/rooms");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void resetRequiredUserCanChangeOwnPassword() throws Exception {
        prepareResetRequiredUser();
        var request = authenticatedRequest("PUT", "/users/me/password");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private void prepareResetRequiredUser() {
        when(jwtUtil.extractUserId("access-token")).thenReturn("user-a");
        when(cachedUserService.getUser("user-a")).thenReturn(User.builder()
                .accountId(1L)
                .userId("user-a")
                .siteId("site-a")
                .userRole(UserRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .passwordResetRequired(true)
                .build());
    }

    private MockHttpServletRequest authenticatedRequest(String method, String path) {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.addHeader("Authorization", "Bearer access-token");
        return request;
    }
}
