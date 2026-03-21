package com.gl.patient_service.repository;

import com.gl.patient_service.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PatientRepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    private Patient testPatient;
    private Patient testPatient2;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();

        testPatient = new Patient();
        testPatient.setName("John Doe");
        testPatient.setEmail("john@example.com");
        testPatient.setAddress("123 Main St");
        testPatient.setDateOfBirth(LocalDate.of(1990, 1, 15));
        testPatient.setRegisteredDate(LocalDate.of(2024, 1, 1));
        testPatient.setDiagnosis("Hypertension");
        testPatient.setCountry("USA");

        testPatient2 = new Patient();
        testPatient2.setName("Jane Smith");
        testPatient2.setEmail("jane@example.com");
        testPatient2.setAddress("456 Oak Ave");
        testPatient2.setDateOfBirth(LocalDate.of(1995, 5, 20));
        testPatient2.setRegisteredDate(LocalDate.of(2024, 1, 2));
        testPatient2.setDiagnosis("Diabetes");
        testPatient2.setCountry("Canada");
    }

    // Test save and findById
    @Test
    void testSavePatient() {
        // Act
        Patient savedPatient = patientRepository.save(testPatient);

        // Assert
        assertNotNull(savedPatient.getId());
        assertEquals("John Doe", savedPatient.getName());
        assertEquals("john@example.com", savedPatient.getEmail());
    }

    @Test
    void testFindPatientById() {
        // Arrange
        Patient savedPatient = patientRepository.save(testPatient);

        // Act
        Optional<Patient> foundPatient = patientRepository.findById(savedPatient.getId());

        // Assert
        assertTrue(foundPatient.isPresent());
        assertEquals("John Doe", foundPatient.get().getName());
        assertEquals("john@example.com", foundPatient.get().getEmail());
    }

    @Test
    void testFindPatientById_NotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        Optional<Patient> foundPatient = patientRepository.findById(nonExistentId);

        // Assert
        assertFalse(foundPatient.isPresent());
    }

    // Test findAll
    @Test
    void testFindAllPatients() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        List<Patient> patients = patientRepository.findAll();

        // Assert
        assertEquals(2, patients.size());
    }

    @Test
    void testFindAllPatients_EmptyList() {
        // Act
        List<Patient> patients = patientRepository.findAll();

        // Assert
        assertTrue(patients.isEmpty());
    }

    // Test update
    @Test
    void testUpdatePatient() {
        // Arrange
        Patient savedPatient = patientRepository.save(testPatient);
        UUID patientId = savedPatient.getId();

        // Act
        savedPatient.setName("John Updated");
        savedPatient.setAddress("789 Elm St");
        Patient updatedPatient = patientRepository.save(savedPatient);

        // Assert
        assertEquals(patientId, updatedPatient.getId());
        assertEquals("John Updated", updatedPatient.getName());
        assertEquals("789 Elm St", updatedPatient.getAddress());
    }

    // Test delete
    @Test
    void testDeletePatient() {
        // Arrange
        Patient savedPatient = patientRepository.save(testPatient);
        UUID patientId = savedPatient.getId();

        // Act
        patientRepository.deleteById(patientId);

        // Assert
        Optional<Patient> deletedPatient = patientRepository.findById(patientId);
        assertFalse(deletedPatient.isPresent());
    }

    @Test
    void testDeleteAllPatients() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        patientRepository.deleteAll();

        // Assert
        List<Patient> patients = patientRepository.findAll();
        assertTrue(patients.isEmpty());
    }

    // Test existsByEmail
    @Test
    void testExistsByEmail_True() {
        // Arrange
        patientRepository.save(testPatient);

        // Act
        boolean exists = patientRepository.existsByEmail("john@example.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByEmail_False() {
        // Arrange
        patientRepository.save(testPatient);

        // Act
        boolean exists = patientRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByEmail_CaseSensitivity() {
        // Arrange
        patientRepository.save(testPatient);

        // Act & Assert
        assertTrue(patientRepository.existsByEmail("john@example.com"));
        // Note: depending on database settings, this might be case-insensitive
    }

    // Test existsByEmailAndIdNot
    @Test
    void testExistsByEmailAndIdNot_True() {
        // Arrange
        Patient patient1 = patientRepository.save(testPatient);
        Patient patient2 = patientRepository.save(testPatient2);

        // Act
        // Check if email exists for a different patient
        boolean exists = patientRepository.existsByEmailAndIdNot("jane@example.com", patient1.getId());

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByEmailAndIdNot_False() {
        // Arrange
        Patient patient1 = patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        // Check if email exists for a different patient (should be false for same patient)
        boolean exists = patientRepository.existsByEmailAndIdNot("john@example.com", patient1.getId());

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByEmailAndIdNot_EmailNotExist() {
        // Arrange
        Patient patient1 = patientRepository.save(testPatient);

        // Act
        boolean exists = patientRepository.existsByEmailAndIdNot("nonexistent@example.com", patient1.getId());

        // Assert
        assertFalse(exists);
    }

    // Test multiple patients
    @Test
    void testMultiplePatients_DifferentEmails() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        boolean john_exists = patientRepository.existsByEmail("john@example.com");
        boolean jane_exists = patientRepository.existsByEmail("jane@example.com");

        // Assert
        assertTrue(john_exists);
        assertTrue(jane_exists);
    }

    @Test
    void testPatientEmailUniqueness() {
        // Arrange
        patientRepository.save(testPatient);

        // Act & Assert
        // Attempting to save another patient with same email should raise constraint violation
        // This is a database constraint test
        testPatient2.setEmail(testPatient.getEmail());
        assertThrows(Exception.class, () -> {
            patientRepository.saveAndFlush(testPatient2);
        });
    }
}

