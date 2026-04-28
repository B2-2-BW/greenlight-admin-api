package com.winten.greenlight.admin.api.controller.webhook;

import java.time.LocalDateTime;

public record AlertRequest(
        String system,
        LocalDateTime sentAt,
        String alertType,
        String title,
        String message
) {
}