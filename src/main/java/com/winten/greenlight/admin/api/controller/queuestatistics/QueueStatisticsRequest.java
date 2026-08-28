package com.winten.greenlight.admin.api.controller.queuestatistics;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * @param roomIds 비우면 접근 가능한 LIVE 방 전체
 * @param window 엑셀용 1m/10m/1h. 없으면 조회 기간으로 창을 고른다.
 */
public record QueueStatisticsRequest(
        @NotNull Instant from,
        @NotNull Instant to,
        List<String> roomIds,
        String window
) {
}
