package com.gl.analyticsservice.dto;

public record CountryInfo(
        String name,
        String region,
        String subregion,
        long population,
        String timezone,
        String currency
) {
    public static CountryInfo empty() {
        return new CountryInfo("unknown", "unknown", "unknown", 0, "unknown", "unknown");
    }
}
