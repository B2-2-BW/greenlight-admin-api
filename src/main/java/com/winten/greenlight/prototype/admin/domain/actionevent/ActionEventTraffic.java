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
    private int concurrentUser;
    private int estimatedWaitTime;
    private long timestamp;

    public static ActionEventTraffic empty() {
        return new ActionEventTraffic(0,0,0,0,0,System.currentTimeMillis());
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
    public void addConcurrentUser(int d) {concurrentUser = concurrentUser + d;}
    public void addEstimatedWaitTime(int d) {estimatedWaitTime = estimatedWaitTime + d;}
}