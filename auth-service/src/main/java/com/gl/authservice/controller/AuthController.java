package com.gl.authservice.controller;

import com.gl.authservice.dto.LoginRequestDTO;
import com.gl.authservice.dto.LoginResponseDTO;
import com.gl.authservice.dto.RegisterRequestDTO;
import com.gl.authservice.dto.RegisterResponseDTO;
import com.gl.authservice.exception.InvalidCredentialsException;
import com.gl.authservice.exception.UserDisabledException;
import com.gl.authservice.model.User;
import com.gl.authservice.repository.UserRepository;
import com.gl.authservice.service.AuthService;
import com.gl.authservice.service.TokenBlacklistService;
import com.gl.authservice.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;


    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO registerRequestDTO){
        RegisterResponseDTO registerResponseDTO = authService.register(registerRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponseDTO);
    }


    @Operation(summary = "Generate token on user login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO) {

        LoginResponseDTO response = authService.authenticate(loginRequestDTO);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Logout and blacklist token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "Refresh-Token", required = false) String refreshToken) {
        if(authHeader != null && authHeader.startsWith("Bearer")){
            String token = authHeader.substring(7);
            long expiration = jwtUtil.getRemainingExpiration(token);
            tokenBlacklistService.blackListToken(token,expiration);

            String email = jwtUtil.extractEmail(token);
            tokenBlacklistService.deleteRefreshToken(email);

            if(refreshToken != null){
                long refreshExpiration = jwtUtil.getRemainingExpiration(refreshToken);
                tokenBlacklistService.blackListToken(refreshToken,refreshExpiration);
            }
            log.info("User logged out successfully");
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(
            @RequestHeader("Refresh-Token") String refreshToken) {

        jwtUtil.validateToken(refreshToken);

        // check it's a refresh token not access token
        String type = jwtUtil.extractClaim(refreshToken, "type");
        if (!"REFRESH".equals(type)) {
            throw new InvalidCredentialsException("Invalid refresh token type");
        }

        // extract email and validate against Redis
        String email = jwtUtil.extractEmail(refreshToken);
        if (tokenBlacklistService.isTokenBlackListed(refreshToken)
                || !tokenBlacklistService.isRefreshTokenValid(email, refreshToken)) {
            throw new InvalidCredentialsException("Refresh token is invalid or expired");
        }
        long bannedAt = tokenBlacklistService.getUserBlacklistTimestamp(email);
        if (bannedAt > 0 && jwtUtil.extractIssuedAt(refreshToken) < bannedAt) {
            throw new InvalidCredentialsException("Refresh token is invalid or expired");
        }

        // get user role from DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!user.isEnabled()) {
            throw new UserDisabledException("User account is disabled");
        }

        // generate new access token only
        String newAccessToken = jwtUtil.generateAccessToken(email, user.getRole().name());
        long expiresIn = jwtUtil.getExpiration();

        log.info("Access token refreshed for user: {}", email);
        return ResponseEntity.ok(
                LoginResponseDTO.of(newAccessToken, refreshToken, expiresIn, user.getRole().name())
        );
    }
}
