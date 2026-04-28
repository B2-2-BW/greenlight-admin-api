package com.winten.greenlight.admin.domain.alert;

import com.winten.greenlight.admin.api.controller.webhook.AlertManagerRequest;
import com.winten.greenlight.admin.api.controller.webhook.AlertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final TeamsAlertClient teamsAlertClient;

    public void sendAlert(AlertManagerRequest alertManagerRequest) {
        var body = TeamsMessage.builder()
                .referer("greenlight")
                .content("[Greenlight] 테스트알람: " + alertManagerRequest.getAlerts())
                .notificationType("TEAMS")
                .build();

        teamsAlertClient.sendWithRetry(body, 3);
    }

    public void sendAlert(AlertRequest alertRequest) {

    }
}