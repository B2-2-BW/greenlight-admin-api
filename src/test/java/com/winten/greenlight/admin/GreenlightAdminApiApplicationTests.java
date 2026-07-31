package com.winten.greenlight.admin;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GreenlightAdminApiApplicationTests {
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    void contextLoads() {
    }

    @Test
    void userManagementMapperStatementsAreBound() {
        var configuration = sqlSessionFactory.getConfiguration();
        String namespace = "com.winten.greenlight.admin.db.repository.mapper.user.UserMapper.";

        assertThat(configuration.hasStatement(namespace + "findAllUsers")).isTrue();
        assertThat(configuration.hasStatement(namespace + "findUserByEmail")).isTrue();
        assertThat(configuration.hasStatement(namespace + "findUsersPage")).isTrue();
        assertThat(configuration.hasStatement(namespace + "countUsersByStatus")).isTrue();
        assertThat(configuration.hasStatement(namespace + "updateUserStatus")).isTrue();
        assertThat(configuration.hasStatement(namespace + "updateUserPassword")).isTrue();
        assertThat(configuration.hasStatement(namespace + "resetUserPassword")).isTrue();
    }

    @Test
    void userPageFilterIncludesStatusWhileStatusCountsExcludeIt() {
        var configuration = sqlSessionFactory.getConfiguration();
        String namespace = "com.winten.greenlight.admin.db.repository.mapper.user.UserMapper.";
        Map<String, Object> pageParameters = Map.of(
                "siteId", "site-a",
                "query", "kim",
                "status", "PENDING",
                "role", "USER",
                "limit", 10,
                "offset", 0
        );
        String pageSql = configuration.getMappedStatement(namespace + "findUsersPage")
                .getBoundSql(pageParameters).getSql().replaceAll("\\s+", " ");
        String countSql = configuration.getMappedStatement(namespace + "countUsersByStatus")
                .getBoundSql(Map.of("siteId", "site-a", "query", "kim", "role", "USER"))
                .getSql().replaceAll("\\s+", " ");

        assertThat(pageSql)
                .contains("u.site_id = ?", "u.user_role = ?", "u.account_status = ?");
        assertThat(countSql)
                .contains("u.site_id = ?", "u.user_role = ?")
                .doesNotContain("u.account_status = ?");
    }

}
