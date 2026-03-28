package com.gl.patient_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;
    @Column(nullable = false)
    private String aggregateType;
    @Column(nullable = false)
    private UUID aggregateId;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false)
    private boolean published = false;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column
    private LocalDateTime publishedAt;
    @PrePersist
    protected void onCreat(){
        this.createdAt  = LocalDateTime.now();
    }

    public static OutboxEvent of(Patient patient){
        return OutboxEvent.builder()
                .aggregateType("Patient")
                .aggregateId(patient.getId())
                .eventType("PATIENT_CREATED")
                .payload(buildPayload(patient))
                .published(false)
                .build();
    }

    private static String buildPayload(Patient patient){
        return String.format(
                "{\"patientId\":\"%s\",\"name\":\"%s\",\"email\":\"%s\"," +
                        "\"eventType\":\"PATIENT_CREATED\",\"diagnosis\":\"%s\",\"country\":\"%s\"}",
                patient.getId(),
                patient.getName(),
                patient.getEmail(),
                patient.getDiagnosis() != null ? patient.getDiagnosis() : "",
                patient.getCountry() != null ? patient.getCountry() : ""
        );
    }
}
