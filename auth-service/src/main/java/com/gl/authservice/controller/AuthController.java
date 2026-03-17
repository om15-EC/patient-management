package com.gl.authservice.controller;

import com.gl.authservice.dto.LoginRequestDTO;
import com.gl.authservice.dto.LoginResponseDTO;
import com.gl.authservice.dto.RegisterRequestDTO;
import com.gl.authservice.dto.RegisterResponseDTO;
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
            @RequestHeader("Authorization") String authHeader){
        if(authHeader != null && authHeader.startsWith("Bearer")){
            String token = authHeader.substring(7);
            long expiration = jwtUtil.getRemainingExpiration(token);
            tokenBlacklistService.blackListToken(token,expiration);
            log.info("User logged out successfully");
        }
        return ResponseEntity.noContent().build();
    }
}
