package com.winten.greenlight.admin.domain.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertLog {
    private Long alertId;
    private String fingerprint;
    private String alertname;
    private AlertStatus status;
    private String siteId;
    private String roomId;
    private String labels;
    private String annotations;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private String createdIp;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String updatedIp;
}
