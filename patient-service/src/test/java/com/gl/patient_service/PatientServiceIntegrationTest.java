package com.gl.patient_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.patient_service.dto.PatientRequestDTO;
import com.gl.patient_service.dto.PatientResponseDTO;
import com.gl.patient_service.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    private PatientRequestDTO patientRequestDTO;

    @BeforeEach
    void setUp() {
        // Clean up database
        patientRepository.deleteAll();

        // Create test patient DTO
        patientRequestDTO = new PatientRequestDTO();
        patientRequestDTO.setName("Integration Test Patient");
        patientRequestDTO.setEmail("integration@test.com");
        patientRequestDTO.setAddress("Test Address");
        patientRequestDTO.setDateOfBirth("1990-01-15");
        patientRequestDTO.setRegisteredDate("2024-01-01");
        patientRequestDTO.setDiagnosis("Test Diagnosis");
        patientRequestDTO.setCountry("Test Country");
    }

    @Test
    void testCreatePatientEndToEnd() throws Exception {
        // Act & Assert - Create patient
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Patient"))
                .andExpect(jsonPath("$.email").value("integration@test.com"));
    }

    @Test
    void testGetAllPatientsEndToEnd() throws Exception {
        // Arrange - Create a patient first
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isOk());

        // Act & Assert - Get all patients
        mockMvc.perform(get("/patients")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Integration Test Patient"));
    }

    @Test
    void testUpdatePatientEndToEnd() throws Exception {
        // Arrange - Create a patient first
        String createResponse = mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PatientResponseDTO createdPatient = objectMapper.readValue(createResponse, PatientResponseDTO.class);
        UUID patientId = UUID.fromString(createdPatient.getId());

        // Update the patient
        PatientRequestDTO updateDTO = new PatientRequestDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setEmail("updated@test.com");
        updateDTO.setAddress("Updated Address");
        updateDTO.setDateOfBirth("1995-05-20");

        // Act & Assert - Update patient
        mockMvc.perform(put("/patients/" + patientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@test.com"));
    }

    @Test
    void testPatchPatientEndToEnd() throws Exception {
        // Arrange - Create a patient first
        String createResponse = mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PatientResponseDTO createdPatient = objectMapper.readValue(createResponse, PatientResponseDTO.class);
        UUID patientId = UUID.fromString(createdPatient.getId());

        // Patch with only name
        PatientRequestDTO patchDTO = new PatientRequestDTO();
        patchDTO.setName("Patched Name");

        // Act & Assert - Patch patient
        mockMvc.perform(patch("/patients/" + patientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Patched Name"));
    }

    @Test
    void testDeletePatientEndToEnd() throws Exception {
        // Arrange - Create a patient first
        String createResponse = mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PatientResponseDTO createdPatient = objectMapper.readValue(createResponse, PatientResponseDTO.class);
        UUID patientId = UUID.fromString(createdPatient.getId());

        // Act & Assert - Delete patient
        mockMvc.perform(delete("/patients/" + patientId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify patient is deleted
        mockMvc.perform(get("/patients")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testDuplicateEmailValidation() throws Exception {
        // Arrange - Create first patient
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isOk());

        // Act & Assert - Try to create second patient with same email
        mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testCompletePatientWorkflow() throws Exception {
        // 1. Create a patient
        String createResponse = mockMvc.perform(post("/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequestDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PatientResponseDTO createdPatient = objectMapper.readValue(createResponse, PatientResponseDTO.class);
        UUID patientId = UUID.fromString(createdPatient.getId());

        // 2. Get all patients
        mockMvc.perform(get("/patients")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("integration@test.com"));

        // 3. Update patient
        PatientRequestDTO updateDTO = new PatientRequestDTO();
        updateDTO.setName("Updated Test Patient");
        updateDTO.setEmail("updated@test.com");
        updateDTO.setAddress("Updated Address");
        updateDTO.setDateOfBirth("1990-01-15");

        mockMvc.perform(put("/patients/" + patientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());

        // 4. Patch patient
        PatientRequestDTO patchDTO = new PatientRequestDTO();
        patchDTO.setDiagnosis("Updated Diagnosis");

        mockMvc.perform(patch("/patients/" + patientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchDTO)))
                .andExpect(status().isOk());

        // 5. Delete patient
        mockMvc.perform(delete("/patients/" + patientId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}

