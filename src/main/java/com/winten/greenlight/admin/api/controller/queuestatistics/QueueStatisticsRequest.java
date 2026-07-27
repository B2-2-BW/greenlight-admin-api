package com.winten.greenlight.admin.api.controller.queuestatistics;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record QueueStatisticsRequest(@NotNull Instant from, @NotNull Instant to, List<String> roomIds) {
}
