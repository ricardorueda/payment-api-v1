package com.payments.api.adapter.in.rest.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AuditLoggingFilter extends OncePerRequestFilter {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;

    public AuditLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            logAccessEvent(request, response, durationMs);
        }
    }

    private void logAccessEvent(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            String clientId = "anonymous";
            String clientName = null;
            String scope = null;

            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
                clientId = jwt.getSubject();
                clientName = jwt.getClaimAsString("client_name");
                scope = jwt.getClaimAsString("scope");
            } else if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                clientId = auth.getName();
                scope = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(" "));
            }

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "ACCESS");
            event.put("event", "API_REQUEST");
            event.put("clientId", clientId);
            if (clientName != null) {
                event.put("clientName", clientName);
            }
            if (scope != null) {
                event.put("scope", scope);
            }
            event.put("method", request.getMethod());
            event.put("path", request.getRequestURI());
            event.put("status", response.getStatus());
            event.put("sourceIp", request.getRemoteAddr());
            event.put("durationMs", durationMs);
            event.put("timestamp", Instant.now().toString());

            AUDIT.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            logger.warn("Failed to write audit log for access event", e);
        }
    }
}
