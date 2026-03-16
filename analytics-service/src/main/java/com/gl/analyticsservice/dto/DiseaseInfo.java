package com.gl.analyticsservice.dto;

import java.util.List;

public record DiseaseInfo(
        String title,
        String description
) {
    public static DiseaseInfo empty() {
        return new DiseaseInfo("unknown", "unavailable");
    }
}
