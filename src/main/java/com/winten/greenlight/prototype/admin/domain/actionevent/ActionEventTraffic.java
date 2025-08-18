package com.winten.greenlight.prototype.admin.domain.actionevent;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActionEventTraffic {
    private int requestCount;
    private int waitingCount;
    private int enteredCount;
    private double requestAverageCount;
    private double enteredAverageCount;
    private int activeUserCount;
    private int concurrentUser;
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
    public void addRequestAverage(double d) {
        requestAverageCount = requestAverageCount + d;
    }
    public void addEnteredAverage(double d) {
        enteredAverageCount = enteredAverageCount + d;
    }
    public void addActiveUser(int d) {
        activeUserCount = activeUserCount + d;
    }
    public void addConcurrentUser(int d) {concurrentUser = concurrentUser + d;}
    public void addEstimatedWaitTime(int d) {estimatedWaitTime = estimatedWaitTime + d;}
}