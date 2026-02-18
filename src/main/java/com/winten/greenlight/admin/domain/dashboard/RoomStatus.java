package com.winten.greenlight.admin.domain.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomStatus {
    private String roomId;
    private int estimatedWaitTime;
    private int waitingCount;
    private int roomCapacity;
    private int roomCustomerCount;
    private double inflowRate;
    private double outflowRate;
}
