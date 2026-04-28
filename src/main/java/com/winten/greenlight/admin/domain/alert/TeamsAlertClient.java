package com.winten.greenlight.admin.domain.alert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamsAlertClient {
    @Value("${teams.alert.url}")
    private String teamsAlertUrl;

    @Value("${teams.alert.token}")
    private String teamsAlertToken;

    private static final String alertPath = "/api/openapi/v1/co/sys/notification";
    private final RestClient restClient = RestClient.builder().build();

    public void sendWithRetry(TeamsMessage message, int retryCount) {
        Exception lastError;
        for (int i = 1; i < retryCount; i++) { // retry count 계산 시 덜 헷갈리게 1부터 시작
            try {
                restClient.post()
                        .uri(teamsAlertUrl + alertPath)
                        .header("X-OPENAPI-KEY", teamsAlertToken)
                        .body(message)
                        .retrieve()
                        .toBodilessEntity();

                log.info("Alert success");
                return;
            } catch (Exception e) {
                lastError = e;
                log.warn("Alert fail attempt={}", i + 1, e);

                if (i == retryCount) {
                    log.error("Final fail: {}", lastError.toString());
                }
            }
        }
    }
}