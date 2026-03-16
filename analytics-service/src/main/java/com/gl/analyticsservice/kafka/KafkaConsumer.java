package com.gl.analyticsservice.kafka;

import com.gl.analyticsservice.dto.EnrichedPatientData;
import com.gl.analyticsservice.service.AnalyticsService;
import com.gl.analyticsservice.service.PatientEnrichmentService;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
    private final AnalyticsService analyticsService;
    private final PatientEnrichmentService enrichmentService;

//    @PostConstruct
//    public void init() {
//        System.out.println("========================================");
//        System.out.println("CONSUMER SERVICE INITIALIZED!");
//        System.out.println("========================================");
//    }

    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void consumeEvent(byte[] event) {
        log.info("=====CONSUMER TRIGGERED========");
//        log.info("Received event size: {} bytes", event.length);
        log.info("Received Kafka event");
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);

            EnrichedPatientData enriched = enrichmentService.enrich(
                    patientEvent.getDiagnosis(),
                    patientEvent.getCountry()
            );

//            saving to db
            analyticsService.recordPatientEven(
                    patientEvent.getPatientId(),
                    patientEvent.getName(),
                    patientEvent.getEmail(),
                    patientEvent.getEventType(),
                    patientEvent.getDiagnosis(),
                    patientEvent.getCountry(),
                    enriched
            );

        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing event {}", e.getMessage(), e);
        }
    }
}
