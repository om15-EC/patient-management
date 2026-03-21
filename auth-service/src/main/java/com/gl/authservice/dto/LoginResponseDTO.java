package com.gl.authservice.dto;

public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String role
) {
    public static LoginResponseDTO of(String accessToken, String refreshToken, long expiresIn, String role){
        return new LoginResponseDTO(accessToken,refreshToken, "Bearer", expiresIn, role);
    }
}
