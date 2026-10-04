package com.gl.patient_service.service;

import com.gl.patient_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxPublishedMarker {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void markPublished(UUID id) {
        outboxEventRepository.findById(id).ifPresent(event -> {
            event.setPublished(true);
            event.setPublishedAt(LocalDateTime.now());
        });
    }
}
