package com.gl.analyticsservice.dto;

import lombok.Builder;

@Builder
public record PatientStatisticsDto(
        long totalPatients,
        long todayRegistrations,
        long thisMonthRegistrations
) { }
