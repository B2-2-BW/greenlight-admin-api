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
public class AlertSendLog {
    private Long sendId;
    private String fingerprint;
    private String alertname;
    private AlertStatus status;
    private String siteId;
    private String roomId;
    private String channel;
    private String target;
    private String message;
    private LocalDateTime sentAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private String createdIp;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String updatedIp;
}
