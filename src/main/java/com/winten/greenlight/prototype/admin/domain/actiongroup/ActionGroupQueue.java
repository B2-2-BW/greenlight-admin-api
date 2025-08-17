package com.winten.greenlight.prototype.admin.domain.actiongroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionGroupQueue {
    private Long actionGroupId;
    private int waitingSize;
    private int sessionSize;
    private int estimatedWaitTime;
}