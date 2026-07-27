package com.winten.greenlight.admin.domain.queuestatistics;

import java.time.Instant;
import java.util.List;

public record QueueStatistics(Instant from, Instant to, long intervalSeconds,
                              List<QueueStatisticsRoomOption> availableRooms, List<QueueStatisticsRoom> series) {
}
