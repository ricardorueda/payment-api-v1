package com.payments.api.adapter.in.rest.security;

import com.payments.api.adapter.in.rest.security.dto.TokenErrorResponse;
import com.payments.api.adapter.in.rest.security.dto.TokenRequest;
import com.payments.api.adapter.in.rest.security.dto.TokenResponse;
import com.payments.api.application.port.in.security.AuthenticateClientUseCase;
import com.payments.api.application.port.in.security.TokenResult;
import com.payments.api.domain.exception.InvalidClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/oauth2/token")
public class TokenController {

    private final AuthenticateClientUseCase authenticateClientUseCase;

    public TokenController(AuthenticateClientUseCase authenticateClientUseCase) {
        this.authenticateClientUseCase = authenticateClientUseCase;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> issueToken(
        @RequestParam(value = "grant_type", required = false) String grantType,
        @RequestParam(value = "client_id", required = false) String clientId,
        @RequestParam(value = "client_secret", required = false) String clientSecret,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (grantType == null || !grantType.equals("client_credentials")) {
            return ResponseEntity.badRequest().body(
                new TokenErrorResponse(
                    "unsupported_grant_type",
                    "Only client_credentials grant type is supported"
                )
            );
        }

        String resolvedClientId = clientId;
        String resolvedClientSecret = clientSecret;

        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String[] extracted = extractBasicAuthCredentials(authHeader);
            if (extracted != null) {
                resolvedClientId = extracted[0];
                resolvedClientSecret = extracted[1];
            }
        }

        if (resolvedClientId == null || resolvedClientSecret == null
            || resolvedClientId.isBlank() || resolvedClientSecret.isBlank()) {
            return ResponseEntity.badRequest().body(
                new TokenErrorResponse("invalid_request", "Missing client credentials")
            );
        }

        TokenRequest tokenRequest = new TokenRequest(grantType, resolvedClientId, resolvedClientSecret);

        try {
            TokenResult result = authenticateClientUseCase.authenticate(
                tokenRequest.getClientId(), tokenRequest.getClientSecret()
            );
            return ResponseEntity.ok(
                new TokenResponse(
                    result.accessToken(),
                    result.tokenType(),
                    result.expiresIn(),
                    result.scope()
                )
            );
        } catch (InvalidClientException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new TokenErrorResponse("invalid_client", "Invalid client credentials")
            );
        }
    }

    private String[] extractBasicAuthCredentials(String authHeader) {
        try {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            String decoded = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            int colonIndex = decoded.indexOf(':');
            if (colonIndex <= 0) {
                return null;
            }
            String extractedClientId = decoded.substring(0, colonIndex);
            String extractedClientSecret = decoded.substring(colonIndex + 1);
            if (extractedClientId.isBlank() || extractedClientSecret.isBlank()) {
                return null;
            }
            return new String[]{extractedClientId, extractedClientSecret};
        } catch (Exception e) {
            return null;
        }
    }
}
