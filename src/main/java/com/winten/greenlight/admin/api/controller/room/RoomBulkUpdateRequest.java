package com.winten.greenlight.admin.api.controller.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record RoomBulkUpdateRequest(
        @NotEmpty List<@NotBlank String> roomIds,
        @PositiveOrZero Integer capacity,
        @PositiveOrZero Integer maxTrafficPerSecond,
        Boolean enabled
) {
}
