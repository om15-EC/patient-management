package com.gl.patient_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private final static long TTL_HOURS = 24;

    public Optional<String> getExistingResponse(String idempotencyKey){
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        String cachedResponse = redisTemplate.opsForValue().get(key);
        if (cachedResponse != null){
            log.info("Idempotency key found — returning cached response for key: {}",
                    idempotencyKey);
            return Optional.of(cachedResponse);
        }
        return Optional.empty();
    }

    public void saveResponse(String idempotencyKey, String response){
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(
                key,
                response,
                TTL_HOURS,
                TimeUnit.HOURS
        );
        log.info("Idempotency response cached for key: {}", idempotencyKey);
    }


}
