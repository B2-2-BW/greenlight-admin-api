package com.winten.greenlight.admin.domain.queuestatistics;

import java.time.Instant;

public record QueueStatisticsPoint(
        Instant timestamp,
        Long roomCapacity,
        Long totalWaiting,
        Long totalActive,
        Long estimatedWaitTime,
        Long waitingCount,
        Long enteredCount,
        Long exitedCount,
        Long cancelledCount
) {
}
