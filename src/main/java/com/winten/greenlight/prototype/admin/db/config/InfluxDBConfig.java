package com.winten.greenlight.prototype.admin.db.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.WriteApi;
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
    @Value("${influxdb.bucket}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClientReactive() {
        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(influxUrl)
                .authenticateToken(token.toCharArray())
                .org(org)
                .bucket(bucket)
                .build();
        return InfluxDBClientFactory.create(options);
    }

    @Bean
    public WriteApi writeReactiveApi(InfluxDBClient client) {
        // 반응형 쓰기 API를 Bean으로 등록
        return client.makeWriteApi();
    }
}