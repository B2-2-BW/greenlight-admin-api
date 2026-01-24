package com.winten.greenlight.admin.db.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPluginConfig {
    @Bean
    public AuditInterceptor auditTenantInterceptor() {
        return new AuditInterceptor();
    }
}