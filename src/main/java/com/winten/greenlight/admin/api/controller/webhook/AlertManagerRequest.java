package com.winten.greenlight.admin.api.controller.webhook;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class AlertManagerRequest {
    private String version;
    private String status;
    private String receiver;
    private List<Alert> alerts;
    private Map<String, String> commonLabels;
    private Map<String, String> commonAnnotations;
    private String externalURL;

    @Data
    public static class Alert {
        private String status;
        private Map<String, String> labels;
        private Map<String, String> annotations;
        private String startsAt;
        private String endsAt;
        private String generatorURL;
    }
}