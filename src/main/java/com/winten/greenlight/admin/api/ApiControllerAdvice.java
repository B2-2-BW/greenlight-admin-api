package com.winten.greenlight.admin.api;

import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorResponse;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.error.NotModifiedException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiControllerAdvice {

    private final LettuceConnectionFactory lettuceConnectionFactory;

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ErrorResponse> handleCoreException(CoreException ex) {
        HttpStatus status = ex.getErrorType().getStatus();
        if (status.is5xxServerError()) {
            if (ex.getDetail() instanceof Throwable cause) {
                log.error("Internal error", cause);
            } else {
                log.error("Internal error: {}", ex.getDetail(), ex);
            }
            return internalError(status);
        }
        Object detail = ex.getDetail() instanceof Throwable ? null : ex.getDetail();
        return ResponseEntity.status(status).body(new ErrorResponse(
                status, ex.getErrorType().getMessage(), detail, LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                HttpStatus.BAD_REQUEST, "요청 값의 형식이 올바르지 않습니다.", null, LocalDateTime.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                HttpStatus.BAD_REQUEST, "요청 본문의 형식이 올바르지 않습니다.", null, LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        var coreException = CoreException.of(ErrorType.INVALID_DATA, errors);
        return handleCoreException(coreException);
    }

    @ExceptionHandler(PSQLException.class)
    public ResponseEntity<ErrorResponse> handlePSQLException(PSQLException ex) {
        log.error("Postgresql Error: {}", ex.getMessage(), ex);
        return internalError();
    }

    @ExceptionHandler(RedisCommandTimeoutException.class)
    public ResponseEntity<ErrorResponse> redisCommandTimeoutExceptionHandler(RedisCommandTimeoutException ex) {
        lettuceConnectionFactory.resetConnection();
        log.error("Redis command timed out", ex);
        return internalError();
    }

    @ExceptionHandler(RedisCommandExecutionException.class)
    public ResponseEntity<ErrorResponse> redisCommandExecutionExceptionHandler(RedisCommandExecutionException ex) {
        log.error("Redis command failed", ex);
        return internalError();
    }

    @ExceptionHandler(NotModifiedException.class)
    public ResponseEntity<ErrorResponse> handleNotModifiedException(NotModifiedException ex) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse webError) {
            HttpStatus status = HttpStatus.resolve(webError.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return ResponseEntity.status(status).headers(webError.getHeaders()).body(new ErrorResponse(
                        status, "요청을 처리할 수 없습니다.", null, LocalDateTime.now()));
            }
        }
        log.error("Unhandled API error", ex);
        return internalError();
    }

    private ResponseEntity<ErrorResponse> internalError() {
        return internalError(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> internalError(HttpStatus status) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                status, "알 수 없는 오류가 발생했습니다.", null, LocalDateTime.now()));
    }
}
