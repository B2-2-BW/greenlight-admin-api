package com.winten.greenlight.admin.support.error;

import org.postgresql.util.PSQLException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponse(HttpStatus status, ErrorCode errorCode, String message, Object detail, LocalDateTime timestamp) {
    public ErrorResponse(CoreException exception) {
        this(exception.getErrorType().getStatus(),
                exception.getErrorType().getCode(),
                exception.getErrorType().getMessage(),
                exception.getDetail(),
                LocalDateTime.now()
        );
    }
    public ErrorResponse(PSQLException ex) {
        this(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.E500,
                "PostgreSQL Error",
                ex.getServerErrorMessage(),
                LocalDateTime.now()
        );
    }
}