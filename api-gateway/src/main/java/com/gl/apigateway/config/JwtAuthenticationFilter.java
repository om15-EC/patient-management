package com.gl.apigateway.config;

import com.gl.apigateway.service.TokenBlacklistService;
import com.gl.apigateway.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        if(path.startsWith("/api/v1/auth/")){
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer")){
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            jwtUtil.validateToken(token);
        }catch (ExpiredJwtException e) {
            log.warn("Jwt token expired: {}", e.getMessage());
            return unauthorizedResponse(exchange);
        }catch (Exception e) {
            log.warn("JWT validation failed {}", e.getMessage());
            return unauthorizedResponse(exchange);
        }

        return tokenBlacklistService.isTokenBlackListed(token)
                .defaultIfEmpty(false)
                .flatMap(isBlackListed -> {
                    log.info("Token blacklist check: {}", isBlackListed);
                    if(isBlackListed){
                        log.warn("Blacklisted token used for path: {}", path);
                        return unauthorizedResponse(exchange);
                }
//            extracting user info and passing it as header to downstream services
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRoles(token);

            if(!isAuthorized(role,method)){
                log.warn("Access denied for user: {} with role: {} on {} {}", email, role,method, path);
                return forbiddenResponse(exchange);
            }


            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(r -> r.header("X-User-Email", email)
                            .header("X-User-Role", role))
                    .build();
            log.info("Access granted for user: {} with role {} on {} {}", email, role, method, path);
            return chain.filter(modifiedExchange);

        });
    }


    private boolean isAuthorized(String role, String method){
        if("ROLE_ADMIN".equals(role)){
            return true;
        }
        if("ROLE_USER".equals(role)){
            return "GET".equals(method);
        }
        return false;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange){
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange){
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
