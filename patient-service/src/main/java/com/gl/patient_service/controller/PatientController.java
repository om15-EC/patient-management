package com.gl.patient_service.controller;

import com.gl.patient_service.dto.PatientRequestDTO;
import com.gl.patient_service.dto.PatientResponseDTO;
import com.gl.patient_service.dto.validators.CreatePatientValidationGroup;
import com.gl.patient_service.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API for managing Patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "Get Patients")
    public ResponseEntity<List<PatientResponseDTO>> getPatients() {

        List<PatientResponseDTO> patients = patientService.getPatients();
        return ResponseEntity.ok().body(patients);
    }

    @PostMapping
    @Operation(summary = "Create a new Patient")
    public ResponseEntity<PatientResponseDTO> createPatient(@Validated({Default.class, CreatePatientValidationGroup.class})
                                                                @RequestBody PatientRequestDTO patientRequestDTO) {
        PatientResponseDTO patient = patientService.createPatient(patientRequestDTO);

        return ResponseEntity.ok().body(patient);
    }

    @PutMapping("/{id}")
    @Operation(summary = "update a patient")
    public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id, @Validated({Default.class}) @RequestBody PatientRequestDTO patientRequestDTO){
        PatientResponseDTO updatePatient = patientService.updatePatient(id, patientRequestDTO);

        return ResponseEntity.ok().body(updatePatient);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "update a patient")
    public ResponseEntity<PatientResponseDTO> patchPatient(@PathVariable UUID id,
                                                           @RequestBody PatientRequestDTO patientRequestDTO) throws BadRequestException {
        PatientResponseDTO patientResponseDTO = patientService.patchPatient(id, patientRequestDTO);
        return ResponseEntity.ok().body(patientResponseDTO);
    }



    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a patient")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id){
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

}
