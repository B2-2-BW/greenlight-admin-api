package com.winten.greenlight.prototype.admin.domain.actionevent;

import com.winten.greenlight.prototype.admin.domain.customer.WaitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

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