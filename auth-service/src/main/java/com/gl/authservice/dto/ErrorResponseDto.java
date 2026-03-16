package com.gl.authservice.dto;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
)
{
    public static ErrorResponseDto of(int status, String error, String message) {
        return new ErrorResponseDto(status,error,message,LocalDateTime.now());
    }
}
