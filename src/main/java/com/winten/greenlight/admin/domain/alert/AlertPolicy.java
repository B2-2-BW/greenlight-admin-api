package com.winten.greenlight.admin.domain.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertPolicy {
    private String siteId;
    private int forTicks;
    private int repeatIntervalSeconds;
    private QueueAlertCompare waitingCompare;
    private Double waitingThreshold;
    private QueueAlertCompare activeCompare;
    private Double activeThreshold;
    private int surgeWaitTimeSeconds;
    private String createdBy;
    private String createdIp;
    private String updatedBy;
    private String updatedIp;

    public static AlertPolicy defaults(String siteId) {
        return AlertPolicy.builder()
                .siteId(siteId)
                .forTicks(2)
                .repeatIntervalSeconds(3600)
                .waitingCompare(QueueAlertCompare.COUNT)
                .waitingThreshold(1d)
                .activeCompare(QueueAlertCompare.COUNT)
                .activeThreshold(1d)
                .surgeWaitTimeSeconds(60)
                .build();
    }
}
