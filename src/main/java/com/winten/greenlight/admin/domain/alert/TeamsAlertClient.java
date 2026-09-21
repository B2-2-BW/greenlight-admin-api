package com.winten.greenlight.admin.domain.alert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamsAlertClient {
    private static final String ALERT_PATH = "/api/openapi/v1/co/sys/notification";
    private final TeamsAlertProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public void sendWithRetry(TeamsMessage message, int retryCount) {
        log.info("Teams alert payload={}", message);
        if (!properties.isEnabled()) {
            log.info("Teams alert skipped (teams.alert.enabled=false)");
            return;
        }
        if (message.getTargets() == null || message.getTargets().isEmpty()) {
            log.warn("Teams alert skipped: targets empty");
            return;
        }

        Exception lastError = null;
        for (int i = 1; i < retryCount; i++) {
            try {
                restClient.post()
                        .uri(properties.getUrl() + ALERT_PATH)
                        .header("X-OPENAPI-KEY", properties.getToken())
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
