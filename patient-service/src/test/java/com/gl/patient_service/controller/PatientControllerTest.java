package com.gl.patient_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.patient_service.dto.PageResponseDTO;
import com.gl.patient_service.dto.PatientRequestDTO;
import com.gl.patient_service.dto.PatientResponseDTO;
import com.gl.patient_service.exception.EmailAlreadyExistException;
import com.gl.patient_service.exception.GlobalExceptionHandler;
import com.gl.patient_service.exception.PatientNotFoundById;
import com.gl.patient_service.service.IdempotencyService;
import com.gl.patient_service.service.PatientService;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock
    private PatientService patientService;

    @Mock
    private IdempotencyService idempotencyService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PatientController patientController;

    private MockMvc mockMvc;
    private PatientRequestDTO patientRequestDTO;
    private PatientResponseDTO patientResponseDTO;
    private UUID testId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        testId = UUID.randomUUID();

        patientRequestDTO = new PatientRequestDTO();
        patientRequestDTO.setName("John Doe");
        patientRequestDTO.setEmail("john@example.com");
        patientRequestDTO.setAddress("123 Main St");
        patientRequestDTO.setDateOfBirth("1990-01-15");
        patientRequestDTO.setRegisteredDate("2024-01-01");
        patientRequestDTO.setDiagnosis("Hypertension");
        patientRequestDTO.setCountry("USA");

        patientResponseDTO = new PatientResponseDTO();
        patientResponseDTO.setId(testId.toString());
        patientResponseDTO.setName("John Doe");
        patientResponseDTO.setEmail("john@example.com");
        patientResponseDTO.setAddress("123 Main St");
        patientResponseDTO.setDateOfBirth("1990-01-15");
        patientResponseDTO.setDiagnosis("Hypertension");
        patientResponseDTO.setCountry("USA");
    }

    // Test GET /patients
    @Test
    void testGetPatients_Success() throws Exception {
        // Arrange
        List<PatientResponseDTO> patients = List.of(patientResponseDTO);
        PageResponseDTO<PatientResponseDTO> pageResponse = new PageResponseDTO<>();
        pageResponse.setContent(patients);
        pageResponse.setPageNumber(0);
        pageResponse.setPageSize(10);
        pageResponse.setTotalElements(1);
        pageResponse.setTotalPages(1);
        pageResponse.setFirst(true);
        pageResponse.setLast(true);

        when(patientService.getPatients(0, 10, "name", "asc", null, null)).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/patients")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Doe"))
                .andExpect(jsonPath("$.content[0].email").value("john@example.com"));

        verify(patientService, times(1)).getPatients(0, 10, "name", "asc", null, null);
    }

    @Test
    void testGetPatients_EmptyList() throws Exception {
        // Arrange
        PageResponseDTO<PatientResponseDTO> pageResponse = new PageResponseDTO<>();
        pageResponse.setContent(List.of());
        pageResponse.setPageNumber(0);
        pageResponse.setPageSize(10);
        pageResponse.setTotalElements(0);
        pageResponse.setTotalPages(0);
        pageResponse.setFirst(true);
        pageResponse.setLast(true);

        when(patientService.getPatients(0, 10, "name", "asc", null, null)).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/patients")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));

        verify(patientService, times(1)).getPatients(0, 10, "name", "asc", null, null);
    }

    // Test POST /patients
    @Test
    void testCreatePatient_Success() throws Exception {
        // Arrange
        when(patientService.createPatient(any(PatientRequestDTO.class))).thenReturn(patientResponseDTO);

        // Act & Assert
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(patientService, times(1)).createPatient(any(PatientRequestDTO.class));
    }

    @Test
    void testCreatePatient_EmailAlreadyExists() throws Exception {
        // Arrange
        when(patientService.createPatient(any(PatientRequestDTO.class)))
                .thenThrow(new EmailAlreadyExistException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isBadRequest());

        verify(patientService, times(1)).createPatient(any(PatientRequestDTO.class));
    }

    @Test
    void testCreatePatient_InvalidEmail() throws Exception {
        // Arrange
        PatientRequestDTO invalidDTO = new PatientRequestDTO();
        invalidDTO.setName("John Doe");
        invalidDTO.setEmail("invalid-email");
        invalidDTO.setAddress("123 Main St");
        invalidDTO.setDateOfBirth("1990-01-15");
        invalidDTO.setRegisteredDate("2024-01-01");

        // Act & Assert
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(patientService, never()).createPatient(any(PatientRequestDTO.class));
    }

    @Test
    void testCreatePatient_MissingRequiredFields() throws Exception {
        // Arrange
        PatientRequestDTO invalidDTO = new PatientRequestDTO();
        invalidDTO.setName("John Doe");
        // Missing email, address, dateOfBirth, registeredDate

        // Act & Assert
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(patientService, never()).createPatient(any(PatientRequestDTO.class));
    }

    // Test PUT /patients/{id}
    @Test
    void testUpdatePatient_Success() throws Exception {
        // Arrange
        when(patientService.updatePatient(eq(testId), any(PatientRequestDTO.class)))
                .thenReturn(patientResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/patients/" + testId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(patientService, times(1)).updatePatient(eq(testId), any(PatientRequestDTO.class));
    }

    @Test
    void testUpdatePatient_PatientNotFound() throws Exception {
        // Arrange
        when(patientService.updatePatient(eq(testId), any(PatientRequestDTO.class)))
                .thenThrow(new PatientNotFoundById("Patient not found"));

        // Act & Assert
        mockMvc.perform(put("/patients/" + testId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isBadRequest());

        verify(patientService, times(1)).updatePatient(eq(testId), any(PatientRequestDTO.class));
    }

    @Test
    void testUpdatePatient_EmailAlreadyExists() throws Exception {
        // Arrange
        when(patientService.updatePatient(eq(testId), any(PatientRequestDTO.class)))
                .thenThrow(new EmailAlreadyExistException("Email already exists"));

        // Act & Assert
        mockMvc.perform(put("/patients/" + testId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isBadRequest());

        verify(patientService, times(1)).updatePatient(eq(testId), any(PatientRequestDTO.class));
    }

    // Test PATCH /patients/{id}
    @Test
    void testPatchPatient_Success() throws Exception {
        // Arrange
        when(patientService.patchPatient(eq(testId), any(PatientRequestDTO.class)))
                .thenReturn(patientResponseDTO);

        // Act & Assert
        mockMvc.perform(patch("/patients/" + testId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(patientService, times(1)).patchPatient(eq(testId), any(PatientRequestDTO.class));
    }

    @Test
    void testPatchPatient_InvalidDateFormat() throws Exception {
        // Arrange
        PatientRequestDTO invalidDTO = new PatientRequestDTO();
        invalidDTO.setDateOfBirth("invalid-date");

        when(patientService.patchPatient(eq(testId), any(PatientRequestDTO.class)))
                .thenThrow(new BadRequestException("Invalid dateOfBirth format"));

        // Act & Assert
        mockMvc.perform(patch("/patients/" + testId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(patientService, times(1)).patchPatient(eq(testId), any(PatientRequestDTO.class));
    }

    // Test DELETE /patients/{id}
    @Test
    void testDeletePatient_Success() throws Exception {
        // Arrange
        doNothing().when(patientService).deletePatient(testId);

        // Act & Assert
        mockMvc.perform(delete("/patients/" + testId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(patientService, times(1)).deletePatient(testId);
    }

    @Test
    void testDeletePatient_InvalidId() throws Exception {
        // Arrange
        String invalidId = "invalid-uuid";

        // Act & Assert
        mockMvc.perform(delete("/patients/" + invalidId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(patientService, never()).deletePatient(any(UUID.class));
    }
}

