package com.winten.greenlight.admin.db.config;

import com.influxdb.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDBConfig {

    @Value("${influxdb.url}")
    private String influxUrl;
    @Value("${influxdb.token}")
    private String token;
    @Value("${influxdb.org}")
    private String org;
    @Value("${influxdb.default-bucket}")
    private String defaultBucket;

    @Bean(destroyMethod = "close")
    public InfluxDBClient influxDBClient() {
        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(influxUrl)
                .authenticateToken(token.toCharArray())
                .org(org)
                .bucket(defaultBucket)
                .build();
        return InfluxDBClientFactory.create(options);
    }

    @Bean
    public WriteApi writeApi(InfluxDBClient client) {
        // 반응형 쓰기 API를 Bean으로 등록
        return client.makeWriteApi();
    }

    @Bean
    public QueryApi queryApi(InfluxDBClient client) {
        return client.getQueryApi();
    }
}