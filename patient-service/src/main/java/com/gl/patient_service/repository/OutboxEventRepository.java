package com.gl.patient_service.repository;

import com.gl.patient_service.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
    List<OutboxEvent> findByPublishedTrueAndPublishedAtBefore(LocalDateTime cutoff);
    Optional<OutboxEvent> findByAggregateId(UUID aggregateId);
}
