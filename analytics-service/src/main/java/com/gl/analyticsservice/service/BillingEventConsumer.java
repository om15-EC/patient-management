package com.gl.analyticsservice.service;

import billing.event.BillingEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingEventConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "billing.events", groupId = "analytics-service")
    public void consumeBillingEvent(byte[] event) {
        try {
            BillingEvent billingEvent = BillingEvent.parseFrom(event);
            log.info("Received billing event: {} for patientId: {}",
                    billingEvent.getStatus(), billingEvent.getPatientId());

            if ("CREATED".equals(billingEvent.getStatus())) {
                emailService.sendWelcomeEmail(
                        billingEvent.getPatientEmail(),
                        billingEvent.getPatientName(),
                        billingEvent.getPatientId(),
                        billingEvent.getAccountId(),
                        billingEvent.getBillingAmount(),
                        "",
                        ""
                );
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing billing event: {}", e.getMessage());
        }
    }
}
