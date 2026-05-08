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

        var title = "[Greenlight] 서비스 다운";

        var contentBuilder = new StringBuilder();
        contentBuilder.append("<b>")
                .append(title)
                .append("</b>");

        for (var alert: alertManagerRequest.getAlerts()) {
            var description = alert.getAnnotations().get("summary");
            contentBuilder.append("<br> - ").append(description);
        };

        var body = TeamsMessage.builder()
                .referer("greenlight")
                .content(contentBuilder.toString())
                .notificationType("TEAMS")
                .build();

        teamsAlertClient.sendWithRetry(body, 3);
    }

    public void sendAlert(AlertRequest alertRequest) {

    }
}