package com.winten.greenlight.admin.domain.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertChannelTarget {
    private Long targetId;
    private Long accountId;
    private String channel;
    private String target;
    private String createdBy;
    private String createdIp;
    private String updatedBy;
    private String updatedIp;
}
