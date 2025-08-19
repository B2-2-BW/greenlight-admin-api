package com.winten.greenlight.prototype.admin.domain.actionevent.dto;

import com.winten.greenlight.prototype.admin.domain.customer.WaitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActionEvent implements Serializable {
    private WaitStatus eventType;
    private Long actionGroupId;
    private Long actionId;
    private String customerId;
    private String recordId;
    private Long timestamp;
    private Long eventTimestamp;
    private Long recordTimestamp;
    private Long waitTimeMs;
}