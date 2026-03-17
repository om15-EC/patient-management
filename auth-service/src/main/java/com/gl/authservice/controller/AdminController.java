package com.gl.authservice.controller;

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


    @Operation(summary = "Admin force logout a user")
    @PostMapping("/logout/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> forceLogout(@PathVariable String userId) {
        tokenBlacklistService.blacklistUser(userId);
        log.info("Admin force-logged out userId: {}", userId);
        return ResponseEntity.noContent().build();
    }
}
