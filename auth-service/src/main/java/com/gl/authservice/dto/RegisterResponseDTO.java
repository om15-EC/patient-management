package com.gl.authservice.dto;

import java.time.LocalDateTime;

public record RegisterResponseDTO(
    String email,
    String role,
    LocalDateTime createdAt
) { }
