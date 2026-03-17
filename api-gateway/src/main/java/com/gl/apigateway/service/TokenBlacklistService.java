package com.gl.apigateway.service;

import com.gl.apigateway.config.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TokenBlacklistService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    public TokenBlacklistService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private final static String BLACKLIST_PREFIX = "blacklist:";
    private static final String USER_BLACKLIST_PREFIX = "user_blacklist:";

    public Mono<Boolean> isTokenBlackListed(String token){
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key)
                .defaultIfEmpty(false)
                .doOnNext(result -> log.info("Redis hasKey({}) = {}", key, result));
    }

    public Mono<Boolean> isUserBlacklisted(String userId, long tokenIssuedAt) {
        String key = USER_BLACKLIST_PREFIX + userId;
        return redisTemplate.opsForValue().get(key)
                .defaultIfEmpty("0")
                .map(timestamp -> {
                    long blacklistTime = Long.parseLong(timestamp);
                    // token issued BEFORE blacklist timestamp → invalid
                    boolean blacklisted = blacklistTime > 0
                            && tokenIssuedAt < blacklistTime;
                    log.info("User blacklist check userId={} blacklisted={}",
                            userId, blacklisted);
                    return blacklisted;
                });
    }
}
