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
    private static final String USER_BLACKLIST_PREFIX = "user_blacklist:";
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

    public void blacklistUser(String userId){
        String key = USER_BLACKLIST_PREFIX + userId;
        long ttl = 24 * 60 * 60 * 1000L;
        redisTemplate.opsForValue().set(
                key,
                String.valueOf(System.currentTimeMillis()),
                ttl,
                TimeUnit.MILLISECONDS
        );
        log.info("User {} force-logged out by admin", userId);
    }

    public boolean isUserBlacklisted(String userId) {
        String key = USER_BLACKLIST_PREFIX + userId;
        return redisTemplate.hasKey(key);
    }

    public long getUserBlacklistTimestamp(String userId) {
        String key = USER_BLACKLIST_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
