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
    private int roomCapacity;
    private long waitingCount;
    private long activeCustomerCount;
    private long inflow;
    private long entered;
    private long outflow;
    private double estimatedWaitTime;
    private double inflowRate;
    private double enteredRate;
    private double outflowRate;
}