package com.lootSafe.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(String timestamp, String message, String errorCode, List<FieldErrorResponse> fieldErrors) {

    public ErrorResponse(String message, String errorCode, List<FieldErrorResponse> fieldErrors) {
        this(LocalDateTime.now().toString(), message, errorCode, fieldErrors);
    }

    public static ErrorResponse of(String message, String errorCode, List<FieldErrorResponse> fieldErrors) {
        return new ErrorResponse(message, errorCode, fieldErrors);
    }
}
