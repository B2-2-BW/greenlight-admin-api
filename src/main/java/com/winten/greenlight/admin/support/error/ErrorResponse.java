package com.winten.greenlight.admin.support.error;

import org.postgresql.util.PSQLException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponse(HttpStatus status, String message, Object detail, LocalDateTime timestamp) {
    public ErrorResponse(CoreException exception) {
        this(exception.getErrorType().getStatus(),
                exception.getErrorType().getMessage(),
                exception.getDetail(),
                LocalDateTime.now()
        );
    }
    public ErrorResponse(PSQLException ex) {
        this(HttpStatus.INTERNAL_SERVER_ERROR,
                "PostgreSQL Error",
                ex.getServerErrorMessage(),
                LocalDateTime.now()
        );
    }
}