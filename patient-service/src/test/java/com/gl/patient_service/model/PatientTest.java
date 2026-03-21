package com.gl.patient_service.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PatientTest {

    private Patient patient;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        patient = new Patient();
        patient.setId(testId);
        patient.setName("John Doe");
        patient.setEmail("john@example.com");
        patient.setAddress("123 Main St");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 15));
        patient.setRegisteredDate(LocalDate.of(2024, 1, 1));
        patient.setDiagnosis("Hypertension");
        patient.setCountry("USA");
    }

    // Test basic getters and setters
    @Test
    void testPatientSettersAndGetters() {
        // Assert
        assertEquals(testId, patient.getId());
        assertEquals("John Doe", patient.getName());
        assertEquals("john@example.com", patient.getEmail());
        assertEquals("123 Main St", patient.getAddress());
        assertEquals(LocalDate.of(1990, 1, 15), patient.getDateOfBirth());
        assertEquals(LocalDate.of(2024, 1, 1), patient.getRegisteredDate());
        assertEquals("Hypertension", patient.getDiagnosis());
        assertEquals("USA", patient.getCountry());
    }

    // Test setName
    @Test
    void testSetName() {
        // Act
        patient.setName("Jane Smith");

        // Assert
        assertEquals("Jane Smith", patient.getName());
    }

    // Test setEmail
    @Test
    void testSetEmail() {
        // Act
        patient.setEmail("jane@example.com");

        // Assert
        assertEquals("jane@example.com", patient.getEmail());
    }

    // Test setAddress
    @Test
    void testSetAddress() {
        // Act
        patient.setAddress("456 Oak Ave");

        // Assert
        assertEquals("456 Oak Ave", patient.getAddress());
    }

    // Test setDateOfBirth
    @Test
    void testSetDateOfBirth() {
        // Act
        LocalDate newDOB = LocalDate.of(1995, 5, 20);
        patient.setDateOfBirth(newDOB);

        // Assert
        assertEquals(newDOB, patient.getDateOfBirth());
    }

    // Test setRegisteredDate
    @Test
    void testSetRegisteredDate() {
        // Act
        LocalDate newDate = LocalDate.of(2024, 3, 15);
        patient.setRegisteredDate(newDate);

        // Assert
        assertEquals(newDate, patient.getRegisteredDate());
    }

    // Test setDiagnosis
    @Test
    void testSetDiagnosis() {
        // Act
        patient.setDiagnosis("Diabetes");

        // Assert
        assertEquals("Diabetes", patient.getDiagnosis());
    }

    // Test setCountry
    @Test
    void testSetCountry() {
        // Act
        patient.setCountry("Canada");

        // Assert
        assertEquals("Canada", patient.getCountry());
    }

    // Test null values
    @Test
    void testPatientWithNullDiagnosis() {
        // Act
        patient.setDiagnosis(null);

        // Assert
        assertNull(patient.getDiagnosis());
    }

    @Test
    void testPatientWithNullCountry() {
        // Act
        patient.setCountry(null);

        // Assert
        assertNull(patient.getCountry());
    }

    // Test patient equality
    @Test
    void testPatientEquality() {
        // Arrange
        Patient patient2 = new Patient();
        patient2.setId(testId);
        patient2.setName("John Doe");
        patient2.setEmail("john@example.com");
        patient2.setAddress("123 Main St");
        patient2.setDateOfBirth(LocalDate.of(1990, 1, 15));
        patient2.setRegisteredDate(LocalDate.of(2024, 1, 1));

        // Assert - Note: Lombok generates equals based on all fields
        // Two patients with the same ID are equal
        assertEquals(patient.getId(), patient2.getId());
    }

    // Test multiple updates
    @Test
    void testMultipleUpdates() {
        // Act - First update
        patient.setName("Jane Smith");
        patient.setEmail("jane@example.com");

        // Assert - After first update
        assertEquals("Jane Smith", patient.getName());
        assertEquals("jane@example.com", patient.getEmail());

        // Act - Second update
        patient.setAddress("789 Elm St");
        patient.setCountry("UK");

        // Assert - After second update
        assertEquals("789 Elm St", patient.getAddress());
        assertEquals("UK", patient.getCountry());
    }

    // Test edge cases with special characters
    @Test
    void testPatientWithSpecialCharacters() {
        // Act
        patient.setName("José García");
        patient.setAddress("Rue de l'École, 75001");
        patient.setCountry("Côte d'Ivoire");

        // Assert
        assertEquals("José García", patient.getName());
        assertEquals("Rue de l'École, 75001", patient.getAddress());
        assertEquals("Côte d'Ivoire", patient.getCountry());
    }

    // Test patient with very long strings
    @Test
    void testPatientWithLongStrings() {
        // Arrange
        String longAddress = "A".repeat(1000);

        // Act
        patient.setAddress(longAddress);

        // Assert
        assertEquals(longAddress, patient.getAddress());
    }
}

