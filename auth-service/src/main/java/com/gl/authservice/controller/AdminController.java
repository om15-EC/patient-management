package com.gl.authservice.controller;

import com.gl.authservice.repository.UserRepository;
import com.gl.authservice.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;


    @Operation(summary = "Admin force logout a user")
    @PostMapping("/logout/{email}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> forceLogout(@PathVariable String email) {
        tokenBlacklistService.blacklistUser(email);
        log.info("Admin force-logged out userId: {}", email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/disable/{email}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> disableUser(@PathVariable String email) {
        // 1. Disable in DB — permanent
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    user.setEnabled(false);
                    userRepository.save(user);
                });

        // 2. Blacklist in Redis — immediate effect
        tokenBlacklistService.blacklistUser(email);
        log.info("User {} permanently disabled", email);
        return ResponseEntity.noContent().build();
    }
}
