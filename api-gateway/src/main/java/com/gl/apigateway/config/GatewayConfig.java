package com.gl.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public GatewayConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public RedisRateLimiter rateLimiter(){
        return new RedisRateLimiter(5,10,1);
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder){
        return builder.routes()
                .route("auth-service-route", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(rateLimiter())
                                        .setKeyResolver(userKeyResolver())))
                        .uri("lb://auth-service"))

                .route("patient-service-route", r-> r
                        .path("/api/patients/**")
                        .filters(f-> f
                                .stripPrefix(1)
                                .filter(jwtAuthenticationFilter)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(rateLimiter())
                                        .setKeyResolver(userKeyResolver())))
                        .uri("lb://patient-service"))

                .route("api-docs-patient-route", r -> r
                        .path("/api-docs/patients")
                        .filters(f -> f
                                .rewritePath("/api-docs/patients", "/v3/api-docs"))
                        .uri("http://patient-service:4000"))

                .route("analytics-service-route", r-> r
                        .path("/api/analytics/**")
                        .filters(f -> f
//                                .stripPrefix(1)
                                .filter(jwtAuthenticationFilter)
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(rateLimiter())
                                        .setKeyResolver(userKeyResolver())))
                        .uri("lb://analytics-service"))

                .route("auth-admin-route", r -> r
                        .path("/api/admin/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://auth-service"))
                .build();
    }

    @Bean
    public KeyResolver userKeyResolver(){
        return exchange -> {
            String email = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Email");
            if (email != null) return Mono.just(email);
            return Mono.just(
                    exchange.getRequest().getRemoteAddress() != null
                            ? exchange.getRequest().getRemoteAddress()
                            .getAddress().getHostAddress()
                            : "unknown"
            );
        };
    }
}
