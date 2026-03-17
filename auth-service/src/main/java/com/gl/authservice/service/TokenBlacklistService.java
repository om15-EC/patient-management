package com.gl.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private final StringHttpMessageConverter stringHttpMessageConverter;

    public void blackListToken(String token, long expirationMs){
        String key = BLACKLIST_PREFIX + token;

        redisTemplate.opsForValue().set(
                key,
                "blacklisted",
                expirationMs,
                TimeUnit.MILLISECONDS
        );
        log.info("Token blacklisted successfully");
    }

    public boolean isTokenBlackListed(String token){
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
}
