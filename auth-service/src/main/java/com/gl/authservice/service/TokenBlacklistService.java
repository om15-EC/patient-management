package com.gl.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String USER_BLACKLIST_PREFIX = "user_blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private final StringHttpMessageConverter stringHttpMessageConverter;

    private static final long REFRESH_TTL_DAYS = 7;

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

    public void blacklistUser(String email){
        String key = USER_BLACKLIST_PREFIX + email;
        long ttl = 24 * 60 * 60 * 1000L; // 24hrs
        redisTemplate.opsForValue().set(
                key,
                String.valueOf(System.currentTimeMillis())
//                ttl,
//                TimeUnit.MILLISECONDS // removing this because once user is disabled
//                no need to add them again in 24hrs automatically.
        );
        log.info("User {} force-logged out by admin", email);
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

    public void storeRefreshToken(String email, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                REFRESH_TTL_DAYS,
                TimeUnit.DAYS
        );
        log.info("Refresh token stored for user: {} ", email);
    }

    public boolean isRefreshTokenValid(String email, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + email;
        String storedToken = redisTemplate.opsForValue().get(key);
        return refreshToken.equals(storedToken);
    }

    public Optional<String> getRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    public void deleteRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.delete(key);
        log.info("Refresh token deleted for user: {}", email);
    }
}
