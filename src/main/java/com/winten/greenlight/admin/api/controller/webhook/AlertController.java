package com.winten.greenlight.admin.api.controller.webhook;

import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    @Value("${alertmanager.token}")
    private String alertmanagerToken;

    /**
     * AlertManager body 예시
     * <pre>
     * {
     *   "version": "4",
     *   "groupKey": "<group_key>",
     *   "status": "firing|resolved",
     *   "receiver": "<receiver_name>",
     *   "alerts": [
     *     {
     *       "status": "firing|resolved",
     *       "labels": { "alertname": "<name>", "severity": "<severity>", "instance": "<instance>", ... },
     *       "annotations": { "summary": "<summary>", "description": "<description>", ... },
     *       "startsAt": "<rfc3339_time>",
     *       "endsAt": "<rfc3339_time>",
     *       "generatorURL": "<generator_url>",
     *       "fingerprint": "<fingerprint>"
     *     }
     *   ],
     *   "commonLabels": { ... },
     *   "commonAnnotations": { ... },
     *   "externalURL": "<alertmanager_url>"
     * }
     * </pre>
     *
     * AlertManager와의 연동을 위한 WebHook API
     * @param message
     */
    @PostMapping("/alertmanager/webhook")
    public String receiveAlertManagerAlert(
            @RequestHeader("Authorization") String alertTokenHeader,
            @RequestBody AlertManagerRequest message
    ) {
        if (!alertmanagerToken.equals(alertTokenHeader)) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "유효하지 않은 인증정보입니다.");
        }

        System.out.println("[AlertManager] " + message);

        return "OK";
    }

    @PostMapping("/general/webhook")
    public String receiveSchedulerAlert(
            @RequestHeader("Authorization") String alertTokenHeader,
            @RequestBody AlertRequest message
    ) {
        if (!alertmanagerToken.equals(alertTokenHeader)) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "유효하지 않은 인증정보입니다.");
        }

        System.out.println("[AlertManager] " + message);

        return "OK";
    }
}