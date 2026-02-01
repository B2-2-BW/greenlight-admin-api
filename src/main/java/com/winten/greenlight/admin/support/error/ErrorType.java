package com.winten.greenlight.admin.support.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorType {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred.", LogLevel.WARN),
    INVALID_DATA(HttpStatus.BAD_REQUEST, "Data is not valid." , LogLevel.INFO ),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized.", LogLevel.INFO),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden.", LogLevel.INFO),
    REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred while accessing data." , LogLevel.ERROR),
    FAILED_TO_PARSE_JSON(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred while parsing json.", LogLevel.ERROR),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found.", LogLevel.INFO),
    USER_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "User Registration Failed.", LogLevel.ERROR),
    USER_ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "User account locked.", LogLevel.INFO),
    USER_PASSWORD_RESET_REQUIRED(HttpStatus.FORBIDDEN, "User account locked.", LogLevel.INFO),
    USERNAME_EXISTS(HttpStatus.CONFLICT, "Username already exists." , LogLevel.INFO ),

    SITE_NOT_FOUND(HttpStatus.NOT_FOUND, "Site not found.", LogLevel.INFO),

    ACTION_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "Action group not found." , LogLevel.INFO),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Room not found." , LogLevel.INFO),
    NONEMPTY_ACTION_GROUP(HttpStatus.CONFLICT, "Action group has actions.", LogLevel.INFO),

    ACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Action not found." , LogLevel.INFO),
    ACTION_EXISTS(HttpStatus.CONFLICT, "Duplicated action name.", LogLevel.INFO),

    ;

    private final HttpStatus status; //HTTP 응답 코드
    private final String message; // 노출 메시지
    private final LogLevel logLevel;

}