package com.winten.greenlight.prototype.admin.domain.actionevent.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ActionEventTraffic {
    private int requestCount;
    private int waitingCount;
    private int enteredCount;
    private double averageRequestCount;
    private double averageEnteredCount;
    private int estimatedWaitTime;
    private long timestamp;

    public static ActionEventTraffic empty() {
        var empty = new ActionEventTraffic();
        empty.setTimestamp(System.currentTimeMillis());
        return empty;
    }

    public void addRequestCount(int d) {
        requestCount = requestCount + d;
    }
    public void addWaitingCount(int d) {
        waitingCount = waitingCount + d;
    }
    public void addEnteredCount(int d) {
        enteredCount = enteredCount + d;
    }
    public void addAverageRequestCount(double d) {
        averageRequestCount = averageRequestCount + d;
    }
    public void addAverageEnteredCount(double d) {
        averageEnteredCount = averageEnteredCount + d;
    }
    public void addEstimatedWaitTimeCount(int d) {estimatedWaitTime = estimatedWaitTime + d;}
}