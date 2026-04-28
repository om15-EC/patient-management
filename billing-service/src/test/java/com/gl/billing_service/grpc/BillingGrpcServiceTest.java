package com.gl.billing_service.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import com.gl.billing_service.model.BillingAccount;
import com.gl.billing_service.repository.BillingAccountRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BillingGrpcService}.
 *
 * gRPC handler methods receive a {@link StreamObserver}; we mock that
 * observer and capture what was passed to onNext / onError / onCompleted.
 */
@ExtendWith(MockitoExtension.class)
class BillingGrpcServiceTest {

    @Mock
    private BillingAccountRepository billingAccountRepository;

    @Mock
    @SuppressWarnings("unchecked")
    private StreamObserver<BillingResponse> responseObserver;

    @InjectMocks
    private BillingGrpcService billingGrpcService;

    private BillingRequest buildRequest(String patientId, String name, String email) {
        return BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .build();
    }

    private BillingAccount buildSavedAccount(String patientId) {
        return BillingAccount.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .status("ACTIVE")
                .billingAmount(BigDecimal.ZERO)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Happy-path — account does not exist yet
    // ------------------------------------------------------------------ //

    @Test
    void createBillingAccount_shouldSaveAccountAndSendResponse() {
        // Given
        String patientId = "patient-001";
        BillingRequest request = buildRequest(patientId, "Alice", "alice@example.com");
        BillingAccount saved  = buildSavedAccount(patientId);

        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(false);
        when(billingAccountRepository.save(any(BillingAccount.class))).thenReturn(saved);

        // When
        billingGrpcService.createBillingAccount(request, responseObserver);

        // Then
        verify(billingAccountRepository).save(any(BillingAccount.class));
        verify(responseObserver).onNext(any(BillingResponse.class));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    void createBillingAccount_responseShouldContainAccountIdAndStatus() {
        // Given
        String patientId = "patient-002";
        BillingRequest request = buildRequest(patientId, "Bob", "bob@example.com");
        BillingAccount saved  = buildSavedAccount(patientId);

        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(false);
        when(billingAccountRepository.save(any())).thenReturn(saved);

        // When
        billingGrpcService.createBillingAccount(request, responseObserver);

        // Then — capture the response and assert fields
        ArgumentCaptor<BillingResponse> captor =
                ArgumentCaptor.forClass(BillingResponse.class);
        verify(responseObserver).onNext(captor.capture());

        BillingResponse response = captor.getValue();
        assertThat(response.getAccountId()).isEqualTo(saved.getId().toString());
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createBillingAccount_savedAccountShouldHaveCorrectPatientId() {
        // Given
        String patientId = "patient-003";
        BillingRequest request = buildRequest(patientId, "Carol", "carol@example.com");
        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(false);
        when(billingAccountRepository.save(any())).thenReturn(buildSavedAccount(patientId));

        // When
        billingGrpcService.createBillingAccount(request, responseObserver);

        // Then — inspect what was persisted
        ArgumentCaptor<BillingAccount> captor =
                ArgumentCaptor.forClass(BillingAccount.class);
        verify(billingAccountRepository).save(captor.capture());

        BillingAccount persisted = captor.getValue();
        assertThat(persisted.getPatientId()).isEqualTo(patientId);
        assertThat(persisted.getStatus()).isEqualTo("ACTIVE");
        assertThat(persisted.getBillingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ //
    //  Duplicate account — ALREADY_EXISTS error
    // ------------------------------------------------------------------ //

    @Test
    void createBillingAccount_whenAccountAlreadyExists_shouldCallOnError() {
        // Given
        String patientId = "patient-004";
        BillingRequest request = buildRequest(patientId, "Dave", "dave@example.com");
        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(true);

        // When
        billingGrpcService.createBillingAccount(request, responseObserver);

        // Then
        verify(responseObserver).onError(any(StatusRuntimeException.class));
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(billingAccountRepository, never()).save(any());
    }

    @Test
    void createBillingAccount_whenAccountAlreadyExists_errorStatusShouldBeAlreadyExists() {
        // Given
        String patientId = "patient-005";
        when(billingAccountRepository.existsByPatientId(patientId)).thenReturn(true);

        // When
        billingGrpcService.createBillingAccount(
                buildRequest(patientId, "Eve", "eve@example.com"),
                responseObserver);

        // Then
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(captor.capture());

        StatusRuntimeException ex = (StatusRuntimeException) captor.getValue();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.ALREADY_EXISTS.getCode());
    }

    // ------------------------------------------------------------------ //
    //  Repository save is not called if duplicate
    // ------------------------------------------------------------------ //

    @Test
    void createBillingAccount_duplicateAccount_shouldNotPersistAnyNewAccount() {
        // Given
        when(billingAccountRepository.existsByPatientId(any())).thenReturn(true);

        // When
        billingGrpcService.createBillingAccount(
                buildRequest("dup-001", "Frank", "frank@example.com"),
                responseObserver);

        // Then
        verify(billingAccountRepository, never()).save(any(BillingAccount.class));
    }
}
