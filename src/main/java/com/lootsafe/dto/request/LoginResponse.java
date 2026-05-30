package com.lootsafe.dto.request;


public record LoginResponse(
        String userId,
        String token
) {}
