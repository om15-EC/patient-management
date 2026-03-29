package com.gl.billing_service.service;

import billing.event.BillingEvent;
import com.gl.billing_service.model.BillingAccount;
import com.gl.billing_service.repository.BillingAccountRepository;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientEventConsumer {

    private final BillingAccountRepository billingAccountRepository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @KafkaListener(topics = "patient", groupId = "billing-service")
    public void consumerPatientEvent(byte[] event){
        log.info("===== BILLING REQUEST VIA KAFKA ===== ");
        try{
            PatientEvent patientEvent= PatientEvent.parseFrom(event);
            log.info("Received patient event: {} for patientId: {}",
                    patientEvent.getEventType(), patientEvent.getPatientId());
            if("PATIENT_CREATED".equals(patientEvent.getEventType())){
                processPatientCreated(patientEvent);
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing patient event: {}", e.getMessage());
        }
    }

    private void processPatientCreated(PatientEvent event) {
        try {
            if(billingAccountRepository.existsByPatientId(event.getPatientId())){
                log.warn("Billing account already exists for: {}", event.getPatientId());
                publishBillingEvent(event.getPatientId(), "ALREADY_EXISTS", "CREATED", "", event.getEmail(), event.getName(), BigDecimal.ZERO);
                return;
            }

            BillingAccount account = BillingAccount.builder()
                    .patientId(event.getPatientId())
                    .status("ACTIVE")
                    .billingAmount(BigDecimal.ZERO)
                    .build();
            BillingAccount saved = billingAccountRepository.save(account);
            log.info("Billing account created: {}", saved.getId());

            publishBillingEvent(event.getPatientId(), saved.getId().toString(),
                    "CREATED", "",
                    event.getEmail(),
                    event.getName(),
                    saved.getBillingAmount());
        } catch (Exception e) {
            log.error("Failed to create billing account for: {}", event.getPatientId());
            // Publish failure event — triggers compensation in patient-service
            publishBillingEvent(event.getPatientId(), "", "FAILED", e.getMessage(), event.getEmail(), event.getName(), BigDecimal.ZERO);
        }
    }

    private void publishBillingEvent(String patientId, String accountId, String status, String reason,
                                     String patientEmail, String patientName, BigDecimal billingAmount) {
        BillingEvent billingEvent = BillingEvent.newBuilder()
                .setPatientId(patientId)
                .setAccountId(accountId)
                .setStatus(status)
                .setReason(reason)
                .setPatientEmail(patientEmail)
                .setPatientName(patientName)
                .setBillingAmount(billingAmount.toString())
                .build();
        kafkaTemplate.send("billing.events", billingEvent.toByteArray());
        log.info("Billing event published: {} for patientId: {}", status, patientId);
    }
}
