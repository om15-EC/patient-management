package com.gl.analyticsservice.repository;

import com.gl.analyticsservice.model.PatientEventLog;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface PatientEventRepository extends JpaRepository<PatientEventLog, UUID> {

    long countByEventType(String eventType);

    long countByCreatedAtBetween(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

    long countByEventTypeAndCreatedAtBetween(String eventType, LocalDateTime start, LocalDateTime end);

}
