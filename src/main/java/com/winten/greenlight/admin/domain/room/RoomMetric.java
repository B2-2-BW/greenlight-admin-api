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
    private Double predictedExitRate; // 예측한 초당 퇴장 인원. 집계 전에는 null
    private long waitingCount;
    private long enteredCount;
    private long exitedCount;
    private double waitingRate;
    private double enteredRate;
    private double exitedRate;
    private long estimatedWaitTime; // 예상 대기시간(초). 예측할 수 없으면 -1
}
