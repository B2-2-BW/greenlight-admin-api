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

    public void addRequest(int d) {
        requestCount = requestCount + d;
    }
    public void addWaiting(int d) {
        waitingCount = waitingCount + d;
    }
    public void addEntered(int d) {
        enteredCount = enteredCount + d;
    }
    public void addAverageRequest(double d) {
        averageRequestCount = averageRequestCount + d;
    }
    public void addAverageEntered(double d) {
        averageEnteredCount = averageEnteredCount + d;
    }
    public void addEstimatedWaitTime(int d) {estimatedWaitTime = estimatedWaitTime + d;}
}