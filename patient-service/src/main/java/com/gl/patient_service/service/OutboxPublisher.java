package com.gl.patient_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gl.patient_service.model.OutboxEvent;
import com.gl.patient_service.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String , byte[]> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents(){

        List<OutboxEvent> unpublishedEvents = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();

        if(unpublishedEvents.isEmpty()) return;

        log.info("OutboxPublisher found {} unpublished events", unpublishedEvents.size());

        unpublishedEvents.forEach(event -> {
            try {
                PatientEvent patientEvent= buildPatientEvent(event);
                kafkaTemplate.send("patient", patientEvent.toByteArray());
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.info("Outbox event published for patientId: {}", event.getAggregateId());
            }catch (Exception e){
                log.error("failed to publish outbox event: {} error {}",
                        event.getId(), e.getMessage());
            }
        });

    }

    private PatientEvent buildPatientEvent(OutboxEvent event)  throws JsonProcessingException {

        JsonNode payload = objectMapper.readTree(event.getPayload());
        return PatientEvent.newBuilder()
                .setPatientId(payload.path("patientId").asText(""))
                .setName(payload.path("name").asText(""))
                .setEmail(payload.path("email").asText(""))
                .setEventType(payload.path("eventType").asText("PATIENT_CREATED"))
                .setDiagnosis(payload.path("diagnosis").asText(""))
                .setCountry(payload.path("country").asText(""))
                .build();
    }
}
