package com.gl.billing_service.service;

import billing.event.BillingEvent;
import com.gl.billing_service.model.BillingAccount;
import com.gl.billing_service.repository.BillingAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import patient.events.PatientEvent;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PatientEventConsumer}.
 *
 * The consumer listens to the "patient" Kafka topic, deserializes Protobuf
 * events, creates BillingAccounts, and publishes BillingEvents back to
 * "billing.events".  All I/O is mocked.
 */
@ExtendWith(MockitoExtension.class)
class PatientEventConsumerTest {

    @Mock
    private BillingAccountRepository billingAccountRepository;

    @Mock
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    @InjectMocks
    private PatientEventConsumer patientEventConsumer;

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private byte[] buildPatientCreatedEvent(String patientId, String name, String email) {
        return PatientEvent.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .setEventType("PATIENT_CREATED")
                .build()
                .toByteArray();
    }

    private BillingAccount savedAccount(String patientId) {
        return BillingAccount.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .status("ACTIVE")
                .billingAmount(BigDecimal.ZERO)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Happy-path: new patient — account created + CREATED event published
    // ------------------------------------------------------------------ //

    @Test
    @SuppressWarnings("unchecked")
    void consumerPatientEvent_shouldCreateAccountAndPublishCreatedEvent() {
        // Given
        String patientId = "p-001";
        byte[] payload  = buildPatientCreatedEvent(patientId, "Alice", "alice@example.com");

        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(false);
        when(billingAccountRepository.save(any(BillingAccount.class)))
                .thenReturn(savedAccount(patientId));

        // When
        patientEventConsumer.consumerPatientEvent(payload);

        // Then
        verify(billingAccountRepository).save(any(BillingAccount.class));
        verify(kafkaTemplate).send(eq("billing.events"), any(byte[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consumerPatientEvent_persistedAccountShouldBeActive() {
        // Given
        String patientId = "p-002";
        byte[] payload  = buildPatientCreatedEvent(patientId, "Bob", "bob@example.com");

        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(false);
        when(billingAccountRepository.save(any())).thenReturn(savedAccount(patientId));

        // When
        patientEventConsumer.consumerPatientEvent(payload);

        // Then
        ArgumentCaptor<BillingAccount> accountCaptor =
                ArgumentCaptor.forClass(BillingAccount.class);
        verify(billingAccountRepository).save(accountCaptor.capture());

        BillingAccount persisted = accountCaptor.getValue();
        assertThat(persisted.getPatientId()).isEqualTo(patientId);
        assertThat(persisted.getStatus()).isEqualTo("ACTIVE");
        assertThat(persisted.getBillingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ //
    //  Duplicate: account already exists — ALREADY_EXISTS event published
    // ------------------------------------------------------------------ //

    @Test
    @SuppressWarnings("unchecked")
    void consumerPatientEvent_whenAccountAlreadyExists_shouldNotSaveAndPublishAlreadyExistsEvent() {
        // Given
        String patientId = "p-003";
        byte[] payload  = buildPatientCreatedEvent(patientId, "Carol", "carol@example.com");

        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(true);

        // When
        patientEventConsumer.consumerPatientEvent(payload);

        // Then
        verify(billingAccountRepository, never()).save(any());
        verify(kafkaTemplate).send(eq("billing.events"), any(byte[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consumerPatientEvent_duplicateAccountEvent_shouldHaveAlreadyExistsStatus() {
        // Given
        String patientId = "p-004";
        byte[] payload  = buildPatientCreatedEvent(patientId, "Dave", "dave@example.com");
        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(true);

        // When
        patientEventConsumer.consumerPatientEvent(payload);

        // Then — parse the published BillingEvent and assert status
        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(kafkaTemplate).send(eq("billing.events"), payloadCaptor.capture());

        try {
            BillingEvent published = BillingEvent.parseFrom(payloadCaptor.getValue());
            assertThat(published.getStatus()).isEqualTo("ALREADY_EXISTS");
            assertThat(published.getPatientId()).isEqualTo(patientId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse published BillingEvent", e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Failure path — repository throws — FAILED event published
    // ------------------------------------------------------------------ //

    @Test
    @SuppressWarnings("unchecked")
    void consumerPatientEvent_whenRepositoryThrows_shouldPublishFailedEvent() {
        // Given
        String patientId = "p-005";
        byte[] payload  = buildPatientCreatedEvent(patientId, "Eve", "eve@example.com");

        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(false);
        when(billingAccountRepository.save(any()))
                .thenThrow(new RuntimeException("DB connection failed"));

        // When
        patientEventConsumer.consumerPatientEvent(payload);

        // Then
        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(kafkaTemplate).send(eq("billing.events"), payloadCaptor.capture());

        try {
            BillingEvent published = BillingEvent.parseFrom(payloadCaptor.getValue());
            assertThat(published.getStatus()).isEqualTo("FAILED");
            assertThat(published.getPatientId()).isEqualTo(patientId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse published BillingEvent", e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Corrupt payload — should not propagate exception
    // ------------------------------------------------------------------ //

    @Test
    void consumerPatientEvent_withCorruptPayload_shouldNotThrow() {
        // Given
        byte[] corrupt = new byte[]{0x01, 0x02};

        // When / Then
        patientEventConsumer.consumerPatientEvent(corrupt);

        verifyNoInteractions(billingAccountRepository, kafkaTemplate);
    }

    @Test
    void consumerPatientEvent_withEmptyPayload_shouldNotThrow() {
        // Given / When / Then
        patientEventConsumer.consumerPatientEvent(new byte[0]);

        verifyNoInteractions(billingAccountRepository, kafkaTemplate);
    }

    // ------------------------------------------------------------------ //
    //  Non-PATIENT_CREATED event types are ignored
    // ------------------------------------------------------------------ //

    @Test
    void consumerPatientEvent_nonCreatedEventType_shouldNotCreateAccount() {
        // Given — different event type
        byte[] payload = PatientEvent.newBuilder()
                .setPatientId("p-006")
                .setName("Frank")
                .setEmail("frank@example.com")
                .setEventType("PATIENT_UPDATED")
                .build()
                .toByteArray();

        // When
        patientEventConsumer.consumerPatientEvent(payload);

        // Then
        verify(billingAccountRepository, never()).existsByPatientId(any());
        verify(billingAccountRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }
}
