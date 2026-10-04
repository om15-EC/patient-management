package com.gl.patient_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class HeaderAuthFilter extends OncePerRequestFilter {

    static final String EMAIL_HEADER = "X-User-Email";
    static final String ROLE_HEADER = "X-User-Role";
    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final String internalToken;

    public HeaderAuthFilter(String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (tokenMatches(request.getHeader(INTERNAL_TOKEN_HEADER))) {
            String role = request.getHeader(ROLE_HEADER);
            String email = request.getHeader(EMAIL_HEADER);
            if (role != null && email != null) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean tokenMatches(String presented) {
        if (presented == null || internalToken == null || internalToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                internalToken.getBytes(StandardCharsets.UTF_8));
    }
}
