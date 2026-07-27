package com.winten.greenlight.admin.domain.queuestatistics;

import java.util.List;

public record QueueStatisticsRoom(String roomId, String name, List<QueueStatisticsPoint> points) {
}
