package com.winten.greenlight.admin.domain.systemstatus;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerStatusClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void readsExistingSchedulerResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/schedulers", exchange -> {
            var body = """
                    [{
                      "schedulerCode": "relocation",
                      "status": "RUNNING",
                      "delaySeconds": 1,
                      "name": "입장 스케줄러",
                      "description": "입장 처리"
                    }]
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        var jsonMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        var client = new SchedulerStatusClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                jsonMapper,
                httpClient
        );

        var result = client.getSchedulers();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSchedulerCode()).isEqualTo("relocation");
        assertThat(result.getFirst().getStatus()).isEqualTo("RUNNING");
    }
}
