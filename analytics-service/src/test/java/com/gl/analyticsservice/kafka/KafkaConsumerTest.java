package com.gl.analyticsservice.kafka;

import com.gl.analyticsservice.dto.CountryInfo;
import com.gl.analyticsservice.dto.DiseaseInfo;
import com.gl.analyticsservice.dto.EnrichedPatientData;
import com.gl.analyticsservice.service.AnalyticsService;
import com.gl.analyticsservice.service.PatientEnrichmentService;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import patient.events.PatientEvent;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KafkaConsumer}.
 *
 * We build real Protobuf byte[] payloads so the deserialization path is
 * exercised, while the downstream services (AnalyticsService,
 * PatientEnrichmentService) remain mocked.
 */
@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private PatientEnrichmentService enrichmentService;

    @InjectMocks
    private KafkaConsumer kafkaConsumer;

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private byte[] buildEvent(String patientId, String name, String email,
                               String eventType, String diagnosis, String country) {
        return PatientEvent.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .setEventType(eventType)
                .setDiagnosis(diagnosis)
                .setCountry(country)
                .build()
                .toByteArray();
    }

    private EnrichedPatientData stubEnrichment() {
        EnrichedPatientData enriched = new EnrichedPatientData(
                new DiseaseInfo("Flu", "Influenza virus infection"),
                new CountryInfo("USA", "Americas", "Northern America",
                        331000000L, "UTC-5", "USD"));
        when(enrichmentService.enrich(anyString(), anyString())).thenReturn(enriched);
        return enriched;
    }

    // ------------------------------------------------------------------ //
    //  Happy-path
    // ------------------------------------------------------------------ //

    @Test
    void consumeEvent_shouldDeserializeAndDelegateToAnalyticsService() {
        // Given
        byte[] payload = buildEvent("p-001", "Alice", "alice@example.com",
                "PATIENT_CREATED", "Flu", "USA");
        EnrichedPatientData enriched = stubEnrichment();

        // When
        kafkaConsumer.consumeEvent(payload);

        // Then
        verify(enrichmentService).enrich("Flu", "USA");
        verify(analyticsService).recordPatientEven(
                eq("p-001"), eq("Alice"), eq("alice@example.com"),
                eq("PATIENT_CREATED"), eq("Flu"), eq("USA"), eq(enriched));
    }

    @Test
    void consumeEvent_shouldPassCorrectPatientIdToEnrichment() {
        // Given
        byte[] payload = buildEvent("p-999", "Bob", "bob@example.com",
                "PATIENT_CREATED", "Diabetes", "India");
        stubEnrichment();

        // When
        kafkaConsumer.consumeEvent(payload);

        // Then
        verify(analyticsService).recordPatientEven(
                eq("p-999"), anyString(), anyString(),
                anyString(), anyString(), anyString(), any());
    }

    // ------------------------------------------------------------------ //
    //  Invalid / corrupt payload — should NOT propagate exception
    // ------------------------------------------------------------------ //

    @Test
    void consumeEvent_withCorruptPayload_shouldNotPropagateException() {
        // Given — garbage bytes that cannot be parsed as Protobuf
        byte[] corruptPayload = new byte[]{0x01, 0x02, 0x03};

        // When / Then — must not throw; consumer must be fault-tolerant
        kafkaConsumer.consumeEvent(corruptPayload);

        // The downstream services must never be called
        verifyNoInteractions(analyticsService, enrichmentService);
    }

    @Test
    void consumeEvent_withEmptyPayload_shouldNotPropagateException() {
        // Given
        byte[] empty = new byte[0];

        // When / Then
        kafkaConsumer.consumeEvent(empty);

        verifyNoInteractions(analyticsService, enrichmentService);
    }

    // ------------------------------------------------------------------ //
    //  Enrichment called exactly once per event
    // ------------------------------------------------------------------ //

    @Test
    void consumeEvent_shouldCallEnrichmentExactlyOnce() {
        // Given
        byte[] payload = buildEvent("p-002", "Carol", "carol@example.com",
                "PATIENT_CREATED", "Asthma", "UK");
        stubEnrichment();

        // When
        kafkaConsumer.consumeEvent(payload);

        // Then
        verify(enrichmentService, times(1)).enrich("Asthma", "UK");
    }
}
