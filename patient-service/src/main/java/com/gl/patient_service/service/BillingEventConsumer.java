package com.gl.patient_service.service;

import billing.event.BillingEvent;
import com.gl.patient_service.model.PatientStatus;
import com.gl.patient_service.repository.PatientRepository;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingEventConsumer {

    private final PatientRepository patientRepository;

    @KafkaListener(topics = "billing.events", groupId = "patient-service")
    public void consumeBillingService(byte[] events) {
        try {
            BillingEvent billingEvent = BillingEvent.parseFrom(events);
            log.info("Received billing event: {} for patientId: {}",
                    billingEvent.getStatus(), billingEvent.getPatientId());

            UUID patientId = UUID.fromString(billingEvent.getPatientId());

            if("CREATED".equals(billingEvent.getStatus())){
                patientRepository.findById(patientId).ifPresent(
                        patient -> {
                            if (patient.getStatus() != PatientStatus.PENDING) {
                                log.warn("Ignoring CREATED for patient {} already in status {}",
                                        patientId, patient.getStatus());
                                return;
                            }
                            patient.setStatus(PatientStatus.ACTIVE);
                            patientRepository.save(patient);
                            log.info("Patient {} marked ACTIVE", patientId);
                        }
                );
            } else if ("FAILED".equals(billingEvent.getStatus())) {
                patientRepository.findById(patientId).ifPresent(
                        patient -> {
                            if (patient.getStatus() != PatientStatus.PENDING) {
                                log.warn("Ignoring FAILED for patient {} already in status {}",
                                        patientId, patient.getStatus());
                                return;
                            }
                            patient.setStatus(PatientStatus.FAILED);
                            patientRepository.save(patient);
                            log.warn("Patient {} marked FAILED — billing failed: {}",
                                    patientId, billingEvent.getReason());
                            patientRepository.deleteById(patientId);
                        }
                );
            }
        }catch (InvalidProtocolBufferException e){
            log.error("Error deserializing billing event: {}", e.getMessage());
        }
    }

}
