package com.gl.patient_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.patient_service.dto.PageResponseDTO;
import com.gl.patient_service.dto.PatientRequestDTO;
import com.gl.patient_service.dto.PatientResponseDTO;
import com.gl.patient_service.dto.validators.CreatePatientValidationGroup;
import com.gl.patient_service.model.PatientStatus;
import com.gl.patient_service.service.IdempotencyService;
import com.gl.patient_service.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API for managing Patients")
public class PatientController {

    private final PatientService patientService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public PatientController(PatientService patientService, IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.patientService = patientService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "Get Patients")
    public ResponseEntity<PageResponseDTO<PatientResponseDTO>> getPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String diagnosis,
            @RequestParam(required = false) PatientStatus status
    ) {

        PageResponseDTO<PatientResponseDTO> patients = patientService.getPatients(page, size, sortBy, sortDir,
                diagnosis, status);
        return ResponseEntity.ok().body(patients);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get Patient")
    public ResponseEntity<PatientResponseDTO> getPatient(@PathVariable UUID id) {

        PatientResponseDTO patient = patientService.getPatientById(id);
        return ResponseEntity.ok().body(patient);
    }

    @PostMapping
    @Operation(summary = "Create a new Patient")
    public ResponseEntity<PatientResponseDTO> createPatient(@Validated({Default.class, CreatePatientValidationGroup.class})
                                                                @RequestBody PatientRequestDTO patientRequestDTO,
                                                            @RequestHeader(value = "Idempotency-Key", required = false)
                                                            String idempotencyKey) throws JsonProcessingException {
        if(idempotencyKey != null && !idempotencyKey.isBlank()){
            Optional<String> cachedResponse =
                    idempotencyService.getExistingResponse(idempotencyKey);

            if(cachedResponse.isPresent()){
                PatientResponseDTO responseDTO = objectMapper.readValue(
                        cachedResponse.get(), PatientResponseDTO.class);
                log.info("Returning cached response for idempotency key: {}",
                        idempotencyKey);
                return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            }
        }

        PatientResponseDTO patient = patientService.createPatient(patientRequestDTO);
        if(idempotencyKey != null && !idempotencyKey.isBlank()){
            idempotencyService.saveResponse(
                    idempotencyKey,
                    objectMapper.writeValueAsString(patient)
            );
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(patient);
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
