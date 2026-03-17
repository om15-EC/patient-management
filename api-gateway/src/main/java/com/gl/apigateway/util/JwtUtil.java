package com.gl.apigateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
    }

    public void validateToken(String token){
//        log.info("Using secret: {}", secretKey); // for testing TO:DO delete later
        Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
    }

    public String  extractEmail(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractRoles(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public long extractIssuedAt(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getIssuedAt()
                .getTime();
    }


}
