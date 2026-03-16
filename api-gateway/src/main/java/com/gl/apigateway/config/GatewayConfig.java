package com.gl.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public GatewayConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder){
        return builder.routes()
                .route("auth-service-route", r -> r
                        .path("/api/v1/auth/**")
                        .uri("lb://auth-service"))

                .route("patient-service-route", r-> r
                        .path("/api/patients/**")
                        .filters(f-> f
                                .stripPrefix(1)
                                .filter(jwtAuthenticationFilter))
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
                                .filter(jwtAuthenticationFilter))
                        .uri("lb://analytics-service"))
                .build();
    }
}
