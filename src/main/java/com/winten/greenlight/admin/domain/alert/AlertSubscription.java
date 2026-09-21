package com.winten.greenlight.admin.domain.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSubscription {
    private Long accountId;
    private String alertname;
    private String label;
    private boolean enabled;
    private String createdBy;
    private String createdIp;
    private String updatedBy;
    private String updatedIp;
}
