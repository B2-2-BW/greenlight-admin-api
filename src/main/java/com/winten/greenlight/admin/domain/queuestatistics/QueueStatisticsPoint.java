package com.winten.greenlight.admin.domain.queuestatistics;

import java.time.Instant;

/**
 * @param totalWaiting 해당 시각 대기 인원(게이지)
 * @param totalActive 해당 시각 체류 인원(게이지)
 * @param waitingCount 창 동안 유입 건수(카운터)
 * @param enteredCount 창 동안 입장 건수(카운터)
 * @param exitedCount 창 동안 이탈 건수(카운터)
 * @param cancelledCount 창 동안 취소 건수(카운터)
 */
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
