package com.winten.greenlight.admin.api.controller.webhook;

import com.winten.greenlight.admin.domain.alert.AlertService;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    @Value("${alertmanager.token}")
    private String alertmanagerToken;

    private final AlertService alertService;

    /**
     * AlertManager body 예시
     * <pre>
     * {
     *   "version": "4",
     *   "groupKey": "&lt;group_key&gt;",
     *   "status": "firing|resolved",
     *   "receiver": "&lt;receiver_name&gt;",
     *   "alerts": [
     *     {
     *       "status": "firing|resolved",
     *       "labels": { "alertname": "&lt;name&gt;", "severity": "&lt;severity&gt;", "instance": "&lt;instance&gt;", ... },
     *       "annotations": { "summary": "&lt;summary&gt;", "description": "&lt;description&gt;", ... },
     *       "startsAt": "&lt;rfc3339_time&gt;",
     *       "endsAt": "&lt;rfc3339_time&gt;",
     *       "generatorURL": "&lt;generator_url&gt;",
     *       "fingerprint": "&lt;fingerprint&gt;"
     *     }
     *   ],
     *   "commonLabels": { ... },
     *   "commonAnnotations": { ... },
     *   "externalURL": "&lt;alertmanager_url&gt;"
     * }
     * </pre>
     *
     * AlertManager와의 연동을 위한 WebHook API
     * @param message
     */
    @PostMapping("/alertmanager/webhook")
    public String receiveAlertManagerAlert(
            @RequestHeader("X-ALERT-TOKEN") String alertTokenHeader,
            @RequestBody AlertManagerRequest message
    ) {
        if (!alertmanagerToken.equals(alertTokenHeader)) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "유효하지 않은 인증정보입니다.");
        }

        List<AlertManagerRequest.Alert> alerts = message.getAlerts() == null ? List.of() : message.getAlerts();
        alertService.apply(alerts, "alertmanager");

        return "OK";
    }

    @PostMapping("/general/webhook")
    public String receiveSchedulerAlert(
            @RequestHeader("X-ALERT-TOKEN") String alertTokenHeader,
            @RequestBody AlertWebhookRequest message
    ) {
        if (!alertmanagerToken.equals(alertTokenHeader)) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "유효하지 않은 인증정보입니다.");
        }

        alertService.apply(message.resolveAlerts(), message.resolveCreatedBy());

        return "OK";
    }
}
