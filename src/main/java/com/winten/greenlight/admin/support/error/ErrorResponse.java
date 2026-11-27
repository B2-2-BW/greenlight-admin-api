package com.winten.greenlight.admin.support.error;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponse(HttpStatus status, String message, Object detail, LocalDateTime timestamp, String code) {
    public ErrorResponse(HttpStatus status, String message, Object detail, LocalDateTime timestamp) {
        this(status, message, detail, timestamp, null);
    }

}
