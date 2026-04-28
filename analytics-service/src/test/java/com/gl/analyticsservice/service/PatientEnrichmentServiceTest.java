package com.gl.analyticsservice.service;

import com.gl.analyticsservice.client.CountryApiClient;
import com.gl.analyticsservice.client.DiseaseApiClient;
import com.gl.analyticsservice.dto.CountryInfo;
import com.gl.analyticsservice.dto.DiseaseInfo;
import com.gl.analyticsservice.dto.EnrichedPatientData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PatientEnrichmentService}.
 *
 * The service fans out two async calls (DiseaseApiClient + CountryApiClient)
 * and joins them.  We mock both clients and verify the assembled result.
 */
@ExtendWith(MockitoExtension.class)
class PatientEnrichmentServiceTest {

    @Mock
    private DiseaseApiClient diseaseApiClient;

    @Mock
    private CountryApiClient countryApiClient;

    @InjectMocks
    private PatientEnrichmentService enrichmentService;

    // ------------------------------------------------------------------ //
    //  Happy-path
    // ------------------------------------------------------------------ //

    @Test
    void enrich_shouldReturnCombinedDiseaseAndCountryData() {
        // Given
        DiseaseInfo disease = new DiseaseInfo("Diabetes", "Chronic blood sugar disorder");
        CountryInfo country = new CountryInfo("India", "Asia", "Southern Asia",
                1400000000L, "UTC+5:30", "INR");

        when(diseaseApiClient.getDisease("Diabetes")).thenReturn(disease);
        when(countryApiClient.getCountry("India")).thenReturn(country);

        // When
        EnrichedPatientData result = enrichmentService.enrich("Diabetes", "India");

        // Then
        assertThat(result.diseaseInfo()).isEqualTo(disease);
        assertThat(result.countryInfo()).isEqualTo(country);
    }

    // ------------------------------------------------------------------ //
    //  Fallback when disease API returns empty
    // ------------------------------------------------------------------ //

    @Test
    void enrich_whenDiseaseApiReturnsEmpty_shouldStillReturnCountryData() {
        // Given
        DiseaseInfo empty = DiseaseInfo.empty();
        CountryInfo country = new CountryInfo("Brazil", "Americas", "South America",
                215000000L, "UTC-3", "BRL");

        when(diseaseApiClient.getDisease("Unknown")).thenReturn(empty);
        when(countryApiClient.getCountry("Brazil")).thenReturn(country);

        // When
        EnrichedPatientData result = enrichmentService.enrich("Unknown", "Brazil");

        // Then
        assertThat(result.diseaseInfo().title()).isEqualTo("unknown");
        assertThat(result.countryInfo().name()).isEqualTo("Brazil");
    }

    // ------------------------------------------------------------------ //
    //  Fallback when country API returns empty
    // ------------------------------------------------------------------ //

    @Test
    void enrich_whenCountryApiReturnsEmpty_shouldStillReturnDiseaseData() {
        // Given
        DiseaseInfo disease = new DiseaseInfo("Malaria", "Mosquito-borne illness");
        CountryInfo emptyCountry = CountryInfo.empty();

        when(diseaseApiClient.getDisease("Malaria")).thenReturn(disease);
        when(countryApiClient.getCountry("UnknownLand")).thenReturn(emptyCountry);

        // When
        EnrichedPatientData result = enrichmentService.enrich("Malaria", "UnknownLand");

        // Then
        assertThat(result.diseaseInfo()).isEqualTo(disease);
        assertThat(result.countryInfo().name()).isEqualTo("unknown");
    }

    // ------------------------------------------------------------------ //
    //  Both APIs return empty (full fallback)
    // ------------------------------------------------------------------ //

    @Test
    void enrich_whenBothApisReturnEmpty_shouldReturnEmptyEnrichedData() {
        // Given
        when(diseaseApiClient.getDisease(any())).thenReturn(DiseaseInfo.empty());
        when(countryApiClient.getCountry(any())).thenReturn(CountryInfo.empty());

        // When
        EnrichedPatientData result = enrichmentService.enrich(null, null);

        // Then
        assertThat(result.diseaseInfo().title()).isEqualTo("unknown");
        assertThat(result.countryInfo().name()).isEqualTo("unknown");
    }

    // ------------------------------------------------------------------ //
    //  Both clients must be called exactly once
    // ------------------------------------------------------------------ //

    @Test
    void enrich_shouldCallBothClientExactlyOnce() {
        // Given
        when(diseaseApiClient.getDisease(any())).thenReturn(DiseaseInfo.empty());
        when(countryApiClient.getCountry(any())).thenReturn(CountryInfo.empty());

        // When
        enrichmentService.enrich("flu", "USA");

        // Then
        verify(diseaseApiClient, times(1)).getDisease("flu");
        verify(countryApiClient, times(1)).getCountry("USA");
    }
}
