package com.lootsafe.exception;

public record ApiErrorResponse(
        int status,
        String message
) {
}