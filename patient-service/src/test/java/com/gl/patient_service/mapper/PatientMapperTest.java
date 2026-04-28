package com.gl.patient_service.mapper;

import com.gl.patient_service.dto.PatientRequestDTO;
import com.gl.patient_service.dto.PatientResponseDTO;
import com.gl.patient_service.model.Patient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PatientMapper} — pure static utility class, no mocking needed.
 *
 * Tests verify bidirectional mapping correctness and edge-case handling.
 */
class PatientMapperTest {

    // ------------------------------------------------------------------ //
    //  toDTO
    // ------------------------------------------------------------------ //

    @Test
    void toDTO_shouldMapAllFieldsCorrectly() {
        // Given
        UUID id = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(id);
        patient.setName("Alice");
        patient.setEmail("alice@example.com");
        patient.setAddress("10 Maple Ave");
        patient.setDateOfBirth(LocalDate.of(1990, 6, 15));
        patient.setDiagnosis("Diabetes");
        patient.setCountry("Canada");

        // When
        PatientResponseDTO dto = PatientMapper.toDTO(patient);

        // Then
        assertThat(dto.getId()).isEqualTo(id.toString());
        assertThat(dto.getName()).isEqualTo("Alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getAddress()).isEqualTo("10 Maple Ave");
        assertThat(dto.getDateOfBirth()).isEqualTo("1990-06-15");
        assertThat(dto.getDiagnosis()).isEqualTo("Diabetes");
        assertThat(dto.getCountry()).isEqualTo("Canada");
    }

    @Test
    void toDTO_whenDiagnosisAndCountryAreNull_shouldReturnNulls() {
        // Given
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setName("Bob");
        patient.setEmail("bob@example.com");
        patient.setAddress("20 Oak St");
        patient.setDateOfBirth(LocalDate.of(1975, 1, 1));
        patient.setDiagnosis(null);
        patient.setCountry(null);

        // When
        PatientResponseDTO dto = PatientMapper.toDTO(patient);

        // Then
        assertThat(dto.getDiagnosis()).isNull();
        assertThat(dto.getCountry()).isNull();
    }

    @Test
    void toDTO_dateOfBirthShouldBeIsoFormatted() {
        // Given
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setName("Carol");
        patient.setEmail("carol@example.com");
        patient.setAddress("30 Pine Rd");
        patient.setDateOfBirth(LocalDate.of(2000, 12, 31));

        // When
        PatientResponseDTO dto = PatientMapper.toDTO(patient);

        // Then — ISO-8601 format expected: yyyy-MM-dd
        assertThat(dto.getDateOfBirth()).isEqualTo("2000-12-31");
    }

    // ------------------------------------------------------------------ //
    //  toModel
    // ------------------------------------------------------------------ //

    @Test
    void toModel_shouldMapAllFieldsCorrectly() {
        // Given
        PatientRequestDTO requestDTO = new PatientRequestDTO();
        requestDTO.setName("Dave");
        requestDTO.setEmail("dave@example.com");
        requestDTO.setAddress("40 Birch Blvd");
        requestDTO.setDateOfBirth("1988-03-22");
        requestDTO.setRegisteredDate("2024-01-10");
        requestDTO.setDiagnosis("Hypertension");
        requestDTO.setCountry("USA");

        // When
        Patient patient = PatientMapper.toModel(requestDTO);

        // Then
        assertThat(patient.getName()).isEqualTo("Dave");
        assertThat(patient.getEmail()).isEqualTo("dave@example.com");
        assertThat(patient.getAddress()).isEqualTo("40 Birch Blvd");
        assertThat(patient.getDateOfBirth()).isEqualTo(LocalDate.of(1988, 3, 22));
        assertThat(patient.getRegisteredDate()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(patient.getDiagnosis()).isEqualTo("Hypertension");
        assertThat(patient.getCountry()).isEqualTo("USA");
    }

    @Test
    void toModel_whenOptionalFieldsAreNull_shouldAllowNulls() {
        // Given
        PatientRequestDTO requestDTO = new PatientRequestDTO();
        requestDTO.setName("Eve");
        requestDTO.setEmail("eve@example.com");
        requestDTO.setAddress("50 Cedar Ct");
        requestDTO.setDateOfBirth("1995-07-04");
        requestDTO.setRegisteredDate("2024-05-01");
        requestDTO.setDiagnosis(null);
        requestDTO.setCountry(null);

        // When
        Patient patient = PatientMapper.toModel(requestDTO);

        // Then
        assertThat(patient.getDiagnosis()).isNull();
        assertThat(patient.getCountry()).isNull();
    }

    @Test
    void toModel_withInvalidDateOfBirth_shouldThrowDateTimeParseException() {
        // Given
        PatientRequestDTO requestDTO = new PatientRequestDTO();
        requestDTO.setName("Frank");
        requestDTO.setEmail("frank@example.com");
        requestDTO.setAddress("60 Walnut Way");
        requestDTO.setDateOfBirth("not-a-date");
        requestDTO.setRegisteredDate("2024-01-01");

        // When / Then
        assertThatThrownBy(() -> PatientMapper.toModel(requestDTO))
                .isInstanceOf(Exception.class); // DateTimeParseException
    }

    // ------------------------------------------------------------------ //
    //  Round-trip: toModel → toDTO
    // ------------------------------------------------------------------ //

    @Test
    void roundTrip_toDTOAfterToModel_shouldPreserveStringFields() {
        // Given
        PatientRequestDTO requestDTO = new PatientRequestDTO();
        requestDTO.setName("Grace");
        requestDTO.setEmail("grace@example.com");
        requestDTO.setAddress("70 Spruce Lane");
        requestDTO.setDateOfBirth("1992-11-08");
        requestDTO.setRegisteredDate("2024-02-14");
        requestDTO.setDiagnosis("Asthma");
        requestDTO.setCountry("Australia");

        // When
        Patient patient = PatientMapper.toModel(requestDTO);
        patient.setId(UUID.randomUUID()); // simulate DB-generated id
        PatientResponseDTO dto = PatientMapper.toDTO(patient);

        // Then
        assertThat(dto.getName()).isEqualTo("Grace");
        assertThat(dto.getEmail()).isEqualTo("grace@example.com");
        assertThat(dto.getAddress()).isEqualTo("70 Spruce Lane");
        assertThat(dto.getDateOfBirth()).isEqualTo("1992-11-08");
        assertThat(dto.getDiagnosis()).isEqualTo("Asthma");
        assertThat(dto.getCountry()).isEqualTo("Australia");
    }
}
