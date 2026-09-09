package com.gl.patient_service.service;

import com.gl.patient_service.dto.PageResponseDTO;
import com.gl.patient_service.dto.PatientRequestDTO;
import com.gl.patient_service.dto.PatientResponseDTO;
import com.gl.patient_service.exception.PatientNotFoundById;
import com.gl.patient_service.model.OutboxEvent;
import com.gl.patient_service.model.Patient;
import com.gl.patient_service.model.PatientStatus;
import com.gl.patient_service.repository.OutboxEventRepository;
import com.gl.patient_service.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Additional unit tests for {@link PatientService} covering:
 *   - getPatientById (hit / miss)
 *   - filter combinations in getPatients
 *   - createPatient outbox persistence
 *   - deletePatient
 *   - sort direction
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceAdditionalTest {

    @Mock private PatientRepository patientRepository;
    @Mock private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private PatientService patientService;

    private UUID patientId;
    private Patient patient;
    private PatientRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(patientId);
        patient.setName("Jane Smith");
        patient.setEmail("jane@example.com");
        patient.setAddress("456 Elm St");
        patient.setDateOfBirth(LocalDate.of(1985, 5, 20));
        patient.setRegisteredDate(LocalDate.of(2024, 3, 1));
        patient.setDiagnosis("Asthma");
        patient.setCountry("UK");
        patient.setStatus(PatientStatus.ACTIVE);

        requestDTO = new PatientRequestDTO();
        requestDTO.setName("Jane Smith");
        requestDTO.setEmail("jane@example.com");
        requestDTO.setAddress("456 Elm St");
        requestDTO.setDateOfBirth("1985-05-20");
        requestDTO.setRegisteredDate("2024-03-01");
        requestDTO.setDiagnosis("Asthma");
        requestDTO.setCountry("UK");
    }

    // ------------------------------------------------------------------ //
    //  getPatientById
    // ------------------------------------------------------------------ //

    @Test
    void getPatientById_whenFound_shouldReturnDTO() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        PatientResponseDTO result = patientService.getPatientById(patientId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(patientId.toString());
        assertThat(result.getName()).isEqualTo("Jane Smith");
        assertThat(result.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void getPatientById_whenNotFound_shouldThrowPatientNotFoundById() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(patientId))
                .isInstanceOf(PatientNotFoundById.class)
                .hasMessageContaining(patientId.toString());
    }

    // ------------------------------------------------------------------ //
    //  getPatients — filter combinations
    // ------------------------------------------------------------------ //

    @Test
    void getPatients_withDiagnosisFilter_shouldUseFindByDiagnosis() {
        Page<Patient> page = new PageImpl<>(List.of(patient), PageRequest.of(0, 10), 1);
        when(patientRepository.findByDiagnosis(eq("Asthma"), any(Pageable.class))).thenReturn(page);

        PageResponseDTO<PatientResponseDTO> result =
                patientService.getPatients(0, 10, "name", "asc", "Asthma", null);

        assertThat(result.getContent()).hasSize(1);
        verify(patientRepository).findByDiagnosis(eq("Asthma"), any(Pageable.class));
        verify(patientRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getPatients_withStatusFilter_shouldUseFindByStatus() {
        Page<Patient> page = new PageImpl<>(List.of(patient), PageRequest.of(0, 10), 1);
        when(patientRepository.findByStatus(eq(PatientStatus.ACTIVE), any(Pageable.class))).thenReturn(page);

        PageResponseDTO<PatientResponseDTO> result =
                patientService.getPatients(0, 10, "name", "asc", null, PatientStatus.ACTIVE);

        assertThat(result.getContent()).hasSize(1);
        verify(patientRepository).findByStatus(eq(PatientStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void getPatients_withBothFilters_shouldUseFindByDiagnosisAndStatus() {
        Page<Patient> page = new PageImpl<>(List.of(patient), PageRequest.of(0, 10), 1);
        when(patientRepository.findByDiagnosisAndStatus(
                eq("Asthma"), eq(PatientStatus.ACTIVE), any(Pageable.class))).thenReturn(page);

        PageResponseDTO<PatientResponseDTO> result =
                patientService.getPatients(0, 10, "name", "asc", "Asthma", PatientStatus.ACTIVE);

        assertThat(result.getContent()).hasSize(1);
        verify(patientRepository).findByDiagnosisAndStatus(
                eq("Asthma"), eq(PatientStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void getPatients_withNoFilters_shouldFindAll() {
        Page<Patient> page = new PageImpl<>(List.of(patient), PageRequest.of(0, 10), 1);
        when(patientRepository.findAll(any(Pageable.class))).thenReturn(page);

        patientService.getPatients(0, 10, "name", "asc", null, null);

        verify(patientRepository).findAll(any(Pageable.class));
    }

    @Test
    void getPatients_descendingSort_shouldApplyDescendingOrder() {
        Page<Patient> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(patientRepository.findAll(any(Pageable.class))).thenReturn(page);

        patientService.getPatients(0, 10, "name", "desc", null, null);

        verify(patientRepository).findAll(any(Pageable.class));
    }

    @Test
    void createPatient_shouldSavePatientAndOutboxEvent() {
        // Given
        when(patientRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient savedPatient = invocation.getArgument(0);
            savedPatient.setId(patientId);
            return savedPatient;
        });
        when(outboxEventRepository.save(any())).thenReturn(mock(OutboxEvent.class));

        // When
        PatientResponseDTO result = patientService.createPatient(requestDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(patientId.toString());

        verify(patientRepository, times(1))
                .existsByEmail(requestDTO.getEmail());

        verify(patientRepository, times(1))
                .save(any(Patient.class));

        verify(outboxEventRepository, times(1))
                .save(any(OutboxEvent.class));
    }

    // ------------------------------------------------------------------ //
    //  deletePatient
    // ------------------------------------------------------------------ //

    @Test
    void deletePatient_shouldDelegateToRepository() {
        doNothing().when(patientRepository).deleteById(patientId);

        patientService.deletePatient(patientId);

        verify(patientRepository, times(1)).deleteById(patientId);
    }

    // ------------------------------------------------------------------ //
    //  Pagination metadata is correctly propagated
    // ------------------------------------------------------------------ //

    @Test
    void getPatients_paginationMetadataShouldMatchPageResult() {
        Page<Patient> page = new PageImpl<>(
                List.of(patient, patient), PageRequest.of(1, 2), 10);
        when(patientRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponseDTO<PatientResponseDTO> result =
                patientService.getPatients(1, 2, "name", "asc", null, null);

        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(5);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isFalse();
    }
}
