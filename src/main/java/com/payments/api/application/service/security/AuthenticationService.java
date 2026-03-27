package com.payments.api.application.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.payments.api.application.port.in.security.AuthenticateClientUseCase;
import com.payments.api.application.port.in.security.TokenResult;
import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.config.JwtProperties;
import com.payments.api.domain.exception.InvalidClientException;
import com.payments.api.domain.security.model.OAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService implements AuthenticateClientUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final ClientRepositoryPort clientRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    public AuthenticationService(ClientRepositoryPort clientRepository,
                                 BCryptPasswordEncoder passwordEncoder,
                                 JwtProperties jwtProperties,
                                 ObjectMapper objectMapper) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public TokenResult authenticate(String clientId, String clientSecret) {
        Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);

        if (clientOpt.isEmpty()) {
            logAuthFailure(clientId, "invalid_client");
            throw new InvalidClientException("Invalid client credentials");
        }

        OAuthClient client = clientOpt.get();

        if (!client.isActive()) {
            logAuthFailure(clientId, "invalid_client");
            throw new InvalidClientException("Invalid client credentials");
        }

        if (!passwordEncoder.matches(clientSecret, client.getHashedSecret())) {
            logAuthFailure(clientId, "invalid_client");
            throw new InvalidClientException("Invalid client credentials");
        }

        String jti = UUID.randomUUID().toString();
        String scope = client.getScope().toScopeString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationSeconds() * 1000L);

        String jwt;
        try {
            byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
            MACSigner signer = new MACSigner(keyBytes);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(client.getClientId())
                .claim("scope", scope)
                .claim("client_name", client.getClientName())
                .issuer(jwtProperties.getIssuer())
                .issueTime(now)
                .expirationTime(expiry)
                .jwtID(jti)
                .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(signer);
            jwt = signedJWT.serialize();
        } catch (Exception e) {
            log.error("Failed to sign JWT for client {}", clientId, e);
            throw new RuntimeException("Token generation failed", e);
        }

        clientRepository.updateLastUsedAt(client.getClientId(), LocalDateTime.now());
        logAuthSuccess(client, scope, jti);

        return new TokenResult(jwt, "Bearer", jwtProperties.getExpirationSeconds(), scope);
    }

    private void logAuthSuccess(OAuthClient client, String scope, String jti) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "AUTH");
            event.put("event", "TOKEN_REQUEST");
            event.put("clientId", client.getClientId());
            event.put("clientName", client.getClientName());
            event.put("scope", scope);
            event.put("outcome", "SUCCESS");
            event.put("jti", jti);
            event.put("sourceIp", resolveSourceIp());
            event.put("timestamp", Instant.now().toString());
            AUDIT.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("Failed to write audit log for successful authentication", e);
        }
    }

    private void logAuthFailure(String clientId, String reason) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "AUTH");
            event.put("event", "TOKEN_REQUEST");
            event.put("clientId", clientId);
            event.put("outcome", "FAILURE");
            event.put("reason", reason);
            event.put("sourceIp", resolveSourceIp());
            event.put("timestamp", Instant.now().toString());
            AUDIT.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("Failed to write audit log for failed authentication", e);
        }
    }

    private String resolveSourceIp() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
