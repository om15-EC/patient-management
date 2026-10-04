package com.gl.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Routes that do not run {@link JwtAuthenticationFilter} (auth, api docs) still
 * must not forward a caller-supplied internal token. Authenticated routes set
 * the token themselves after stripping it.
 */
@Component
public class InternalTokenGlobalFilter implements GlobalFilter, Ordered {

    private final String internalToken;

    public InternalTokenGlobalFilter(@Value("${app.security.internal-token:}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/patients") || path.startsWith("/api/analytics") || path.startsWith("/api/admin")) {
            return chain.filter(exchange);
        }
        return chain.filter(stamp(exchange));
    }

    private ServerWebExchange stamp(ServerWebExchange exchange) {
        return exchange.mutate().request(builder -> builder.headers(headers -> {
            headers.remove("X-Internal-Token");
            if (!internalToken.isBlank()) {
                headers.set("X-Internal-Token", internalToken);
            }
        })).build();
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
