package com.gl.analyticsservice.controller;

import com.gl.analyticsservice.dto.PatientStatisticsDto;
import com.gl.analyticsservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<PatientStatisticsDto> getSummary(){
        return ResponseEntity.ok(analyticsService.getStatistics());
    }
}
