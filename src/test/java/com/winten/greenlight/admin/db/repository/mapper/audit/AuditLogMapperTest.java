package com.winten.greenlight.admin.db.repository.mapper.audit;

import com.winten.greenlight.admin.domain.audit.AuditLog;
import com.winten.greenlight.admin.domain.audit.AuditAction;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuditLogMapperTest {
    private static final String NAMESPACE =
            "com.winten.greenlight.admin.db.repository.mapper.audit.AuditLogMapper.";

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void crudActionFiltersUseExactMatches() {
        for (AuditAction action : AuditAction.values()) {
            assertThat(sql("findPage", parameters(action)))
                    .contains("action = ?")
                    .doesNotContain("action NOT IN");
            assertThat(sql("count", parameters(action)))
                    .contains("action = ?")
                    .doesNotContain("action NOT IN");
        }
    }

    @Test
    void insertAndSelectStatementsIncludeSourcePath() {
        String insertSql = sqlSessionFactory.getConfiguration()
                .getMappedStatement(NAMESPACE + "insert")
                .getBoundSql(AuditLog.builder()
                        .requestId("request-1")
                        .sourcePath("/sites/site-a")
                        .targetType("SITE")
                        .targetId("site-a")
                        .action("UPDATE")
                        .reason("수정")
                        .changeDetail("{}")
                        .build())
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();

        assertThat(insertSql).contains("request_id, source_path, target_site_id");
        assertThat(sql("findPage", parameters(null)))
                .contains("audit_id, request_id, source_path, target_site_id");
    }

    @Test
    void auditLogCanRetainHistoricalDetailedActionAsString() {
        AuditLog auditLog = AuditLog.builder()
                .action("APPROVE")
                .build();

        assertThat(auditLog.getAction()).isEqualTo("APPROVE");
    }

    private String sql(String statement, Map<String, Object> parameters) {
        return sqlSessionFactory.getConfiguration()
                .getMappedStatement(NAMESPACE + statement)
                .getBoundSql(parameters)
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Map<String, Object> parameters(AuditAction action) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("targetSiteId", null);
        parameters.put("createdBy", null);
        parameters.put("targetType", null);
        parameters.put("targetId", null);
        parameters.put("action", action == null ? null : action.name());
        parameters.put("from", null);
        parameters.put("to", null);
        parameters.put("limit", 20);
        parameters.put("offset", 0L);
        return parameters;
    }
}
