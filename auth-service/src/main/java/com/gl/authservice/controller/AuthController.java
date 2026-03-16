package com.gl.authservice.controller;

import com.gl.authservice.dto.LoginRequestDTO;
import com.gl.authservice.dto.LoginResponseDTO;
import com.gl.authservice.dto.RegisterRequestDTO;
import com.gl.authservice.dto.RegisterResponseDTO;
import com.gl.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


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
}
