package com.gl.patient_service.service;

import com.gl.patient_service.model.OutboxEvent;
import com.gl.patient_service.model.PatientStatus;
import com.gl.patient_service.repository.OutboxEventRepository;
import com.gl.patient_service.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingPatientExpiry {

    private final OutboxEventRepository outboxEventRepository;
    private final PatientRepository patientRepository;
    private final CacheManager cacheManager;

    @Value("${app.saga.pending-timeout:2m}")
    private Duration pendingTimeout;

    @Scheduled(fixedDelayString = "${app.saga.pending-scan-delay:60000}")
    @Transactional
    public void expirePatientsWaitingOnBilling() {
        LocalDateTime cutoff = LocalDateTime.now().minus(pendingTimeout);
        List<OutboxEvent> sent = outboxEventRepository.findByPublishedTrueAndPublishedAtBefore(cutoff);
        if (sent.isEmpty()) {
            return;
        }

        int expired = 0;
        for (OutboxEvent event : sent) {
            int updated = patientRepository.markFailedIfPending(
                    event.getAggregateId(), PatientStatus.FAILED, PatientStatus.PENDING);
            if (updated > 0) {
                expired++;
                log.warn("Patient {} still PENDING {} after billing event was published — marked FAILED",
                        event.getAggregateId(), pendingTimeout);
            }
        }
        if (expired > 0) {
            evict("patients");
            evict("patient");
        }
    }

    private void evict(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
