package com.gl.analyticsservice.service;

import com.gl.analyticsservice.client.CountryApiClient;
import com.gl.analyticsservice.client.DiseaseApiClient;
import com.gl.analyticsservice.dto.CountryInfo;
import com.gl.analyticsservice.dto.DiseaseInfo;
import com.gl.analyticsservice.dto.EnrichedPatientData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientEnrichmentService {

    private final DiseaseApiClient diseaseApiClient;
    private final CountryApiClient countryApiClient;

    public EnrichedPatientData enrich(String diagnosis, String country){

        CompletableFuture<DiseaseInfo> diseaseFuture = CompletableFuture.supplyAsync(
                () -> diseaseApiClient.getDisease(diagnosis))
                .orTimeout(3, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("Disease API timed out or failed: {}", ex.getMessage());
                    return DiseaseInfo.empty();
                });

        CompletableFuture<CountryInfo> countryFuture =
                CompletableFuture.supplyAsync(() -> countryApiClient.getCountry(country))
                        .orTimeout(3, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("Country API timed out or failed: {}", ex.getMessage());
                            return CountryInfo.empty();
                        });

        CompletableFuture.allOf(diseaseFuture,countryFuture).join();

        return new EnrichedPatientData(
                diseaseFuture.join(),
                countryFuture.join()
        );
    }
}
