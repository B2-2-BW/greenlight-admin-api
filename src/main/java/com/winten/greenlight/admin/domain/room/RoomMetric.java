package com.winten.greenlight.admin.domain.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomMetric {
    private String roomId;
    private long totalWaiting;
    private long totalActive;
    private long waitingCount;
    private long enteredCount;
    private long exitedCount;
    private double waitingRate;
    private double enteredRate;
    private double exitedRate;
    private long estimatedWaitTime;
}