package com.winten.greenlight.admin.db.config;

import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditInterceptorTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void selectKeepsRequestedCreatedByFilter() throws Throwable {
        authenticate("super-user", "root", UserRole.SUPER);
        var statement = mock(MappedStatement.class);
        when(statement.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("createdBy", null);
        var invocation = mock(Invocation.class);
        when(invocation.getArgs()).thenReturn(new Object[]{statement, parameters});
        when(invocation.proceed()).thenReturn(List.of());

        new AuditInterceptor().intercept(invocation);

        assertThat(parameters)
                .containsEntry("createdBy", null)
                .containsEntry("userSiteId", "root")
                .containsEntry("userRole", UserRole.SUPER);
        verify(invocation).proceed();
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
