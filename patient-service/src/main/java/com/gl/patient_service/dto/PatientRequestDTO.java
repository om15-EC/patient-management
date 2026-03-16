package com.gl.patient_service.dto;

import com.gl.patient_service.dto.validators.CreatePatientValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientRequestDTO {
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name cannot exceed 100 char")
    private String name;

    @NotBlank
    @Email(message = "email should be valid")
    private String email;

    @NotBlank
    private String address;

    @NotBlank
    private String dateOfBirth;

    @NotBlank(groups = CreatePatientValidationGroup.class)
    private String registeredDate;

    private String diagnosis;
    private String country;
}
