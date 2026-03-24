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
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
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

    @CacheEvict(value = {"patients","patient"}, allEntries = true)
    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO){
        if(patientRepository.existsByEmail(patientRequestDTO.getEmail())){
            throw new EmailAlreadyExistException("A patient of this email already exist " + patientRequestDTO.getEmail());
        }
        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));
        log.info("patient saved with PENDING status: {}", newPatient.getId());

//        billingServiceGrpcClient.createBillingAccount(
//                newPatient.getId().toString(),
//                newPatient.getName(),
//                newPatient.getEmail());

//      Publish to Kafka — billing-service will pick it up
        kafkaProducer.sendEvent(newPatient);
        log.info("Patient created event published for: {}", newPatient.getId());

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
