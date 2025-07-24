package com.winten.greenlight.prototype.admin.api;

import com.winten.greenlight.prototype.admin.support.error.CoreException;
import com.winten.greenlight.prototype.admin.support.error.ErrorResponse;
import com.winten.greenlight.prototype.admin.support.error.ErrorType;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerAdvice {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ErrorResponse> handleCoreException(CoreException ex) {
        return ResponseEntity.status(ex.getErrorType().getStatus()).body(new ErrorResponse(ex));
    }

    @ExceptionHandler(PSQLException.class)
    public ResponseEntity<ErrorResponse> handlePSQLException(PSQLException ex) {
        log.error("Postgresql Error: {}", ex.getMessage(), ex);
        var error = new ErrorResponse(ex);
        return ResponseEntity.status(error.status()).body(error);
    }

}