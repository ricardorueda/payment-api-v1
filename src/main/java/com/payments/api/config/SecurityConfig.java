package com.payments.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.api.adapter.in.rest.GlobalExceptionHandler;
import com.payments.api.adapter.in.rest.security.AuditLoggingFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtProperties jwtProperties, ObjectMapper objectMapper) {
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public AuditLoggingFilter auditLoggingFilter() {
        return new AuditLoggingFilter(objectMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers ->
                headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/token").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api-docs/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/payments").hasAuthority("SCOPE_write")
                .requestMatchers(HttpMethod.GET, "/api/v1/payments", "/api/v1/payments/**").hasAuthority("SCOPE_read")
                .requestMatchers("/api/v1/clients/**").hasAuthority("SCOPE_client:admin")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    GlobalExceptionHandler.ErrorResponse error = new GlobalExceptionHandler.ErrorResponse(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized",
                        "Full authentication is required to access this resource",
                        LocalDateTime.now()
                    );
                    objectMapper.writeValue(response.getWriter(), error);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    GlobalExceptionHandler.ErrorResponse error = new GlobalExceptionHandler.ErrorResponse(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden",
                        "Insufficient scope for this operation",
                        LocalDateTime.now()
                    );
                    objectMapper.writeValue(response.getWriter(), error);
                })
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    GlobalExceptionHandler.ErrorResponse error = new GlobalExceptionHandler.ErrorResponse(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized",
                        "Full authentication is required to access this resource",
                        LocalDateTime.now()
                    );
                    objectMapper.writeValue(response.getWriter(), error);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    GlobalExceptionHandler.ErrorResponse error = new GlobalExceptionHandler.ErrorResponse(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden",
                        "Insufficient scope for this operation",
                        LocalDateTime.now()
                    );
                    objectMapper.writeValue(response.getWriter(), error);
                })
            )
            .addFilterAfter(auditLoggingFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer());
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }
}
