package com.gl.analyticsservice.dto;

public record EnrichedPatientData(
        DiseaseInfo diseaseInfo,
        CountryInfo countryInfo
) { }
