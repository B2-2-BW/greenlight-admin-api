package com.winten.greenlight.admin;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
        assertThat(configuration.hasStatement(namespace + "updateUserStatus")).isTrue();
        assertThat(configuration.hasStatement(namespace + "updateUserPassword")).isTrue();
        assertThat(configuration.hasStatement(namespace + "resetUserPassword")).isTrue();
    }

}
