package com.winten.greenlight.admin.domain.queuestatistics;

import java.time.Instant;
import java.util.List;

/**
 * @param series 방별 시계열
 * @param maxWaiting 선택한 방의 동시 대기 최댓값. 방별 최대 대기의 합이 아니다.
 * @param maxActive 선택한 방의 동시 체류 최댓값. 방별 최대 체류의 합이 아니다.
 */
public record QueueStatistics(Instant from, Instant to, long intervalSeconds,
                              List<QueueStatisticsRoomOption> availableRooms, List<QueueStatisticsRoom> series,
                              long maxWaiting, long maxActive) {
}
