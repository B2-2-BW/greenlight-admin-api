package com.winten.greenlight.prototype.admin.domain.actiongroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionGroupQueue {
    private Long actionGroupId;
    private int waitingSize;
    private int estimatedWaitTime;
    private int activeUserCount;
}