package com.gl.authservice.dto;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn,
        String role
) {
    public static LoginResponseDTO of(String accessToken, long expiresIn, String role){
        return new LoginResponseDTO(accessToken, "Bearer", expiresIn, role);
    }
}
