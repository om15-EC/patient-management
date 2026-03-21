package com.gl.patient_service.service;

import com.gl.patient_service.dto.PatientRequestDTO;
import com.gl.patient_service.dto.PatientResponseDTO;
import com.gl.patient_service.exception.EmailAlreadyExistException;
import com.gl.patient_service.exception.PatientNotFoundById;
import com.gl.patient_service.grpc.BillingServiceGrpcClient;
import com.gl.patient_service.kafka.KafkaProducer;
import com.gl.patient_service.mapper.PatientMapper;
import com.gl.patient_service.model.Patient;
import com.gl.patient_service.repository.PatientRepository;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private BillingServiceGrpcClient billingServiceGrpcClient;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private PatientService patientService;

    private PatientRequestDTO patientRequestDTO;
    private Patient testPatient;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        
        patientRequestDTO = new PatientRequestDTO();
        patientRequestDTO.setName("John Doe");
        patientRequestDTO.setEmail("john@example.com");
        patientRequestDTO.setAddress("123 Main St");
        patientRequestDTO.setDateOfBirth("1990-01-15");
        patientRequestDTO.setRegisteredDate("2024-01-01");
        patientRequestDTO.setDiagnosis("Hypertension");
        patientRequestDTO.setCountry("USA");

        testPatient = new Patient();
        testPatient.setId(testId);
        testPatient.setName("John Doe");
        testPatient.setEmail("john@example.com");
        testPatient.setAddress("123 Main St");
        testPatient.setDateOfBirth(LocalDate.of(1990, 1, 15));
        testPatient.setRegisteredDate(LocalDate.of(2024, 1, 1));
        testPatient.setDiagnosis("Hypertension");
        testPatient.setCountry("USA");
    }

    // Test getPatients
    @Test
    void testGetPatients_Success() {
        // Arrange
        List<Patient> patients = List.of(testPatient);
        when(patientRepository.findAll()).thenReturn(patients);

        // Act
        List<PatientResponseDTO> result = patientService.getPatients();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(patientRepository, times(1)).findAll();
    }

    @Test
    void testGetPatients_EmptyList() {
        // Arrange
        when(patientRepository.findAll()).thenReturn(List.of());

        // Act
        List<PatientResponseDTO> result = patientService.getPatients();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(patientRepository, times(1)).findAll();
    }

    // Test createPatient
    @Test
    void testCreatePatient_Success() {
        // Arrange
        when(patientRepository.existsByEmail(patientRequestDTO.getEmail())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);
        doNothing().when(billingServiceGrpcClient).createBillingAccount(anyString(), anyString(), anyString());
        doNothing().when(kafkaProducer).sendEvent(any(Patient.class));

        // Act
        PatientResponseDTO result = patientService.createPatient(patientRequestDTO);

        // Assert
        assertNotNull(result);
        verify(patientRepository, times(1)).existsByEmail(patientRequestDTO.getEmail());
        verify(patientRepository, times(1)).save(any(Patient.class));
        verify(billingServiceGrpcClient, times(1)).createBillingAccount(anyString(), anyString(), anyString());
        verify(kafkaProducer, times(1)).sendEvent(any(Patient.class));
    }

    @Test
    void testCreatePatient_EmailAlreadyExists() {
        // Arrange
        when(patientRepository.existsByEmail(patientRequestDTO.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistException.class, () -> {
            patientService.createPatient(patientRequestDTO);
        });
        verify(patientRepository, times(1)).existsByEmail(patientRequestDTO.getEmail());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    // Test updatePatient
    @Test
    void testUpdatePatient_Success() {
        // Arrange
        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), testId)).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        // Act
        PatientResponseDTO result = patientService.updatePatient(testId, patientRequestDTO);

        // Assert
        assertNotNull(result);
        verify(patientRepository, times(1)).findById(testId);
        verify(patientRepository, times(1)).existsByEmailAndIdNot(patientRequestDTO.getEmail(), testId);
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    void testUpdatePatient_PatientNotFound() {
        // Arrange
        when(patientRepository.findById(testId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PatientNotFoundById.class, () -> {
            patientService.updatePatient(testId, patientRequestDTO);
        });
        verify(patientRepository, times(1)).findById(testId);
    }

    @Test
    void testUpdatePatient_EmailAlreadyExists() {
        // Arrange
        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), testId)).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistException.class, () -> {
            patientService.updatePatient(testId, patientRequestDTO);
        });
        verify(patientRepository, times(1)).findById(testId);
        verify(patientRepository, times(1)).existsByEmailAndIdNot(patientRequestDTO.getEmail(), testId);
        verify(patientRepository, never()).save(any(Patient.class));
    }

    // Test patchPatient
    @Test
    void testPatchPatient_Success() throws BadRequestException {
        // Arrange
        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), testId)).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        // Act
        PatientResponseDTO result = patientService.patchPatient(testId, patientRequestDTO);

        // Assert
        assertNotNull(result);
        verify(patientRepository, times(1)).findById(testId);
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    void testPatchPatient_PatientNotFound() {
        // Arrange
        when(patientRepository.findById(testId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PatientNotFoundById.class, () -> {
            patientService.patchPatient(testId, patientRequestDTO);
        });
    }

    @Test
    void testPatchPatient_InvalidDateFormat() throws BadRequestException {
        // Arrange
        patientRequestDTO.setDateOfBirth("invalid-date");
        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), testId)).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            patientService.patchPatient(testId, patientRequestDTO);
        });
    }

    @Test
    void testPatchPatient_PartialUpdate() throws BadRequestException {
        // Arrange
        PatientRequestDTO partialDTO = new PatientRequestDTO();
        partialDTO.setName("Jane Doe");
        partialDTO.setEmail(testPatient.getEmail());

        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.existsByEmailAndIdNot(partialDTO.getEmail(), testId)).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        // Act
        PatientResponseDTO result = patientService.patchPatient(testId, partialDTO);

        // Assert
        assertNotNull(result);
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    // Test deletePatient
    @Test
    void testDeletePatient_Success() {
        // Arrange
        doNothing().when(patientRepository).deleteById(testId);

        // Act
        patientService.deletePatient(testId);

        // Assert
        verify(patientRepository, times(1)).deleteById(testId);
    }

    // Test edge cases
    @Test
    void testPatchPatient_EmptyEmailField() throws BadRequestException {
        // Arrange
        patientRequestDTO.setEmail("  ");
        patientRequestDTO.setName(null);
        patientRequestDTO.setAddress(null);
        patientRequestDTO.setDateOfBirth(null);

        when(patientRepository.findById(testId)).thenReturn(Optional.of(testPatient));
        when(patientRepository.save(any(Patient.class))).thenReturn(testPatient);

        // Act
        PatientResponseDTO result = patientService.patchPatient(testId, patientRequestDTO);

        // Assert
        assertNotNull(result);
        verify(patientRepository, times(1)).save(any(Patient.class));
    }
}

