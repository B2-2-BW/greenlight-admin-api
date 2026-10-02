package com.winten.greenlight.admin.api.controller.webhook;

import com.winten.greenlight.admin.domain.alert.AlertStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class AlertWebhookRequest {
    private String createdBy;
    private List<AlertManagerRequest.Alert> alerts;

    private String system;
    private LocalDateTime sentAt;
    private String alertType;
    private String title;
    private String message;

    public List<AlertManagerRequest.Alert> resolveAlerts() {
        if (alerts != null && !alerts.isEmpty()) {
            return alerts;
        }
        if (alertType == null || alertType.isBlank()) {
            return List.of();
        }
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("alertname", alertType);
        Map<String, String> annotations = new LinkedHashMap<>();
        if (title != null) {
            annotations.put("summary", title);
        }
        if (message != null) {
            annotations.put("description", message);
        }
        AlertManagerRequest.Alert alert = new AlertManagerRequest.Alert();
        alert.setStatus(AlertStatus.FIRING.name());
        alert.setLabels(labels);
        alert.setAnnotations(annotations);
        if (sentAt != null) {
            alert.setStartsAt(sentAt.toString());
        }
        return List.of(alert);
    }

    public String resolveCreatedBy() {
        if (createdBy != null && !createdBy.isBlank()) {
            return createdBy.trim();
        }
        if (system != null && !system.isBlank()) {
            return system.trim();
        }
        return "greenlight-scheduler";
    }
}
