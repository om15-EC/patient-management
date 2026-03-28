package com.gl.patient_service.service;

import com.gl.patient_service.dto.PatientRequestDTO;
import com.gl.patient_service.dto.PatientResponseDTO;
import com.gl.patient_service.exception.EmailAlreadyExistException;
import com.gl.patient_service.exception.PatientNotFoundById;
import com.gl.patient_service.grpc.BillingServiceGrpcClient;
import com.gl.patient_service.kafka.KafkaProducer;
import com.gl.patient_service.mapper.PatientMapper;
import com.gl.patient_service.model.OutboxEvent;
import com.gl.patient_service.model.Patient;
import com.gl.patient_service.model.PatientStatus;
import com.gl.patient_service.repository.OutboxEventRepository;
import com.gl.patient_service.repository.PatientRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;
    private final OutboxEventRepository outboxEventRepository;

    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer, OutboxEventRepository outboxEventRepository) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Cacheable(value = "patients", key = "'all'")
    public List<PatientResponseDTO> getPatients(){
        List<Patient> patients = patientRepository.findAll();
        return patients.stream()
                .map(PatientMapper::toDTO).toList();
    }

    @Cacheable(value = "patient", key = "#id")
    public PatientResponseDTO getPatientById(UUID id) {
        log.info("Cache miss — fetching patient {} from DB", id);
        return patientRepository.findById(id)
                .map(PatientMapper::toDTO)
                .orElseThrow(() -> new PatientNotFoundById("Patient not found: " + id));
    }

    @Transactional
    @CacheEvict(value = {"patients","patient"}, allEntries = true)
    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO){
        if(patientRepository.existsByEmail(patientRequestDTO.getEmail())){
            throw new EmailAlreadyExistException("A patient of this email already exist " + patientRequestDTO.getEmail());
        }
//        saving patient with PENDING status
        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));
        log.info("patient saved with PENDING status: {}", newPatient.getId());
//      saving outbox event as SAME transaction
        outboxEventRepository.save(OutboxEvent.of(newPatient));
        log.info("Outbox event saved for patientId: {}", newPatient.getId());


//      grpc
        try{
        billingServiceGrpcClient.createBillingAccount(
                newPatient.getId().toString(),
                newPatient.getName(),
                newPatient.getEmail());

//      grpc success, making patient as ACTIVE
        newPatient.setStatus(PatientStatus.ACTIVE);
        patientRepository.save(newPatient);
        log.info("Billing created via gRPC — patient ACTIVE: {}", newPatient.getId());

//      mark outbox as published.
        outboxEventRepository.findByAggregateId(newPatient.getId())
                .ifPresent(event -> {
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                });

        // Still sending analytics event via Kafka
        kafkaProducer.sendEvent(newPatient);
        }catch (Exception e){
            // gRPC failed — OutboxPublisher will handle via Kafka
            log.warn("gRPC failed for patientId: {} — Outbox will handle: {}",
                    newPatient.getId(), e.getMessage());
            // patient stays PENDING — will be ACTIVE via Kafka flow
        }
        return PatientMapper.toDTO(newPatient);
    }

    @CacheEvict(value = {"patients","patient"}, allEntries = true)
    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO){
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundById("patient not found with id" + id));


        if(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(),id )){
            throw new EmailAlreadyExistException("A patient of this email already exist "
                    + patientRequestDTO.getEmail());
        }

        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(patient);
        return PatientMapper.toDTO(updatedPatient);
    }

    @CacheEvict(value = {"patients","patient"}, allEntries = true)
    public PatientResponseDTO patchPatient(UUID id, PatientRequestDTO patientRequestDTO) throws BadRequestException {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundById("patient not found with id" + id));


        if(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(),id )){
            throw new EmailAlreadyExistException("A patient of this email already exist "
                    + patientRequestDTO.getEmail());
        }

        if(patientRequestDTO.getName() != null){
            patient.setName(patientRequestDTO.getName());
        }
        if (patientRequestDTO.getAddress() != null) {
            patient.setAddress(patientRequestDTO.getAddress());
        }

        if (patientRequestDTO.getEmail() != null) {
            String email = patientRequestDTO.getEmail().isBlank() ? null : patientRequestDTO.getEmail();
            patient.setEmail(email);
        }

        if (patientRequestDTO.getDateOfBirth() != null) {
            String dobStr = patientRequestDTO.getDateOfBirth().trim();
            if (dobStr.isEmpty()) {
                throw new BadRequestException("dateOfBirth must be a valid ISO date (yyyy-MM-dd) or omitted");
            }
            try {
                LocalDate dob = LocalDate.parse(dobStr);
                patient.setDateOfBirth(dob);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid dateOfBirth format. Expected yyyy-MM-dd");
            }
        }

        Patient save = patientRepository.save(patient);
        return PatientMapper.toDTO(save);
    }

    @CacheEvict(value = {"patients","patient"}, allEntries = true)
    public void deletePatient(UUID id){
        patientRepository.deleteById(id);
    }



}
