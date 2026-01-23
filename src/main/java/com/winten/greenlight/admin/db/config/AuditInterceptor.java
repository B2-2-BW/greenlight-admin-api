package com.winten.greenlight.admin.db.config;

import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Intercepts({
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
        ),
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}
        )
})
public class AuditInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object param = args.length > 1 ? args[1] : null;
        if (param == null) {
            args[1] = new HashMap<String, Object>();
            param = args[1];
        }
        CurrentUser user = getCurrentUserOrNull();
        String siteId = user != null ? user.getSiteId() : null;
        UserRole userRole = user != null ? user.getUserRole() : UserRole.GUEST; // 일반적으로 회원가입 등의 public CRUD인 경우
        String userId = user != null ? user.getUserId() : null;

        // INSERT/UPDATE/DELETE/UPSERT 등: SqlCommandType으로 분기
        SqlCommandType cmd = ms.getSqlCommandType();

        LocalDateTime now = LocalDateTime.now();
        String ip = RequestScopeUtil.getRequestIp();

        // SELECT, INSERT, UPDATE, DELETE 일 때 넣기
        if (cmd != SqlCommandType.UNKNOWN && cmd != SqlCommandType.FLUSH) {
            // INSERT/UPSERT를 "INSERT로 들어오는 케이스"로 처리
            applyAudit(param, siteId, userRole, userId, now, ip);
        } else {
            // DELETE 등은 정책에 따라 처리(여기선 siteId만 주입)
            if (siteId != null) {
                setField(param, "siteId", siteId);
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this); // MyBatis interceptor 기본 패턴
    }

    @Override
    public void setProperties(Properties properties) {
        // no-op
    }

    private void applyAudit(Object param, String siteId, UserRole userRole, String userId, LocalDateTime now, String ip) {
        // INSERT는 created + updated 둘 다 채우는 정책(요구사항)
        setField(param, "siteId", siteId);
        setField(param, "userRole", userRole);

        setField(param, "createdBy", userId);
        setField(param, "createdAt", now);
        setField(param, "createdIp", ip);

        setField(param, "updatedBy", userId);
        setField(param, "updatedAt", now);
        setField(param, "updatedIp", ip);
    }

    /**
     * param이
     * - @Param을 사용한 경우: MapperMethod.ParamMap / Map
     * - 엔티티 단일 파라미터인 경우: 엔티티 객체
     * 둘 다 처리.
     */
    @SuppressWarnings("unchecked")
    private void setField(Object param, String fieldName, Object value) {
        if (param == null || value == null) return;

        // 1) Map/ParamMap이면 key로 주입
        if (param instanceof MapperMethod.ParamMap<?> p) {
            ((MapperMethod.ParamMap<Object>) p).put(fieldName, value);
            return;
        }
        if (param instanceof Map<?, ?> m) {
            ((Map<String, Object>) m).put(fieldName, value);
            return;
        }

        // 2) 엔티티면 해당 프로퍼티가 있을 때만 set
        MetaObject metaObject = SystemMetaObject.forObject(param);
        if (metaObject.hasSetter(fieldName)) {
            metaObject.setValue(fieldName, value);
        }
    }

    // ---- Security / Request ----

    private CurrentUser getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal instanceof CurrentUser cu) return cu;

        return null;
    }
}