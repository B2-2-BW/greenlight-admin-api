package com.winten.greenlight.admin.db.config;

import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomEntity;
import com.winten.greenlight.admin.domain.audit.AuditLog;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void selectKeepsRequestedCreatedByFilter() throws Throwable {
        authenticate("super-user", "root", UserRole.SUPER);
        var statement = mock(MappedStatement.class);
        when(statement.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("createdBy", null);
        when(statement.getBoundSql(parameters)).thenAnswer(ignored -> {
            assertThat(parameters)
                    .containsEntry("userSiteId", "root")
                    .containsEntry("userRole", UserRole.SUPER);
            return boundSql("SELECT * FROM audit_log", parameters);
        });
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

    @Test
    void selectMappedUpdateReturningReceivesAuditFields() throws Throwable {
        authenticate("super-user", "root", UserRole.SUPER);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var room = RoomEntity.builder().roomId("room-a").build();
        var statement = mock(MappedStatement.class);
        when(statement.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);
        when(statement.getBoundSql(room)).thenReturn(boundSql(
                "UPDATE room SET updated_by = ? WHERE room_id = ? RETURNING *",
                room
        ));
        var invocation = mock(Invocation.class);
        when(invocation.getArgs()).thenReturn(new Object[]{statement, room});
        when(invocation.proceed()).thenReturn(room);

        new AuditInterceptor().intercept(invocation);

        assertThat(room.getUpdatedBy()).isEqualTo("super-user");
        assertThat(room.getUpdatedIp()).isEqualTo("127.0.0.1");
        assertThat(room.getUserRole()).isEqualTo(UserRole.SUPER);
        verify(invocation).proceed();
    }

    @Test
    void auditInsertReceivesSuperActorFields() throws Throwable {
        authenticate("super-user", "root", UserRole.SUPER);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var auditLog = AuditLog.builder()
                .targetSiteId("site-a")
                .targetType("ROOM")
                .targetId("room-a")
                .action("UPDATE")
                .build();
        var statement = mock(MappedStatement.class);
        when(statement.getSqlCommandType()).thenReturn(SqlCommandType.INSERT);
        var invocation = mock(Invocation.class);
        when(invocation.getArgs()).thenReturn(new Object[]{statement, auditLog});
        when(invocation.proceed()).thenReturn(1);

        new AuditInterceptor().intercept(invocation);

        assertThat(auditLog.getCreatedBy()).isEqualTo("super-user");
        assertThat(auditLog.getCreatedIp()).isEqualTo("127.0.0.1");
        assertThat(auditLog.getCreatedAt()).isNotNull();
        verify(invocation).proceed();
    }

    private BoundSql boundSql(String sql, Object parameter) {
        return new BoundSql(new Configuration(), sql, List.of(), parameter);
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
