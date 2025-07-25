package com.winten.greenlight.prototype.admin.support.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorType {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "An unexpected error has occurred.", LogLevel.WARN),
    INVALID_DATA(HttpStatus.BAD_REQUEST, ErrorCode.E400, "Data is not valid." , LogLevel.INFO ),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, ErrorCode.E401, "Unauthorized.", LogLevel.INFO),
    FORBIDDEN(HttpStatus.FORBIDDEN, ErrorCode.E403, "Forbidden.", LogLevel.INFO),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E404, "User not found.", LogLevel.INFO),
    ACTION_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E404 , "Action not found." , LogLevel.INFO),
    ACTION_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E404 , "Action group not found." , LogLevel.INFO),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E404, "Event not found.", LogLevel.INFO),
    NONEMPTY_ACTION_GROUP(HttpStatus.CONFLICT, ErrorCode.E409, "Action group has actions.", LogLevel.INFO),
    ACTION_EXISTS(HttpStatus.CONFLICT, ErrorCode.E409, "Duplicated event name.", LogLevel.INFO),
    REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "An unexpected error has occurred while accessing data." , LogLevel.WARN );

    private final HttpStatus status; //HTTP 응답 코드
    private final ErrorCode code; // 고유 오류 코드
    private final String message; // 노출 메시지
    private final LogLevel logLevel;

}