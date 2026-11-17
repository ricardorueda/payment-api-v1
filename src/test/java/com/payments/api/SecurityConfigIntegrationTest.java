package com.payments.api;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "jwt.secret-key=test-secret-key-for-integration-tests-hmac-sha256-at-least-32-bytes",
    "jwt.issuer=payments-api",
    "jwt.expiration-seconds=1800"
})
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    private static final String TEST_SECRET =
        "test-secret-key-for-integration-tests-hmac-sha256-at-least-32-bytes";

    @Autowired
    private MockMvc mockMvc;

    // region: helper JWT builders

    private String createToken(String scope) throws Exception {
        return buildJwt(scope, new Date(System.currentTimeMillis() + 30 * 60 * 1000));
    }

    private String createExpiredToken(String scope) throws Exception {
        return buildJwt(scope, new Date(System.currentTimeMillis() - 60 * 1000));
    }

    private String createTokenWithWrongSignature(String scope) throws Exception {
        String wrongSecret = "wrong-secret-key-that-does-not-match-the-app-secret-at-all";
        byte[] wrongKeyBytes = wrongSecret.getBytes(StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(wrongKeyBytes);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject("test-client")
            .issuer("payments-api")
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 30 * 60 * 1000))
            .claim("scope", scope)
            .claim("client_name", "test-service")
            .jwtID(UUID.randomUUID().toString())
            .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(signer);
        return jwt.serialize();
    }

    private String buildJwt(String scope, Date expiresAt) throws Exception {
        byte[] keyBytes = TEST_SECRET.getBytes(StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(keyBytes);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject("test-client-id")
            .issuer("payments-api")
            .issueTime(new Date())
            .expirationTime(expiresAt)
            .claim("scope", scope)
            .claim("client_name", "test-service")
            .jwtID(UUID.randomUUID().toString())
            .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(signer);
        return jwt.serialize();
    }

    // endregion

    // region: unauthenticated requests → 401

    @Test
    void unauthenticatedGetPaymentsReturns401WithJsonBody() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value("Full authentication is required to access this resource"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unauthenticatedPostPaymentsReturns401WithJsonBody() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100.00, \"paymentMethod\": \"PIX\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value("Full authentication is required to access this resource"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unauthenticatedGetClientsReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
            .andExpect(status().isUnauthorized());
    }

    // endregion

    // region: read scope enforcement

    @Test
    void readScopeTokenCanAccessGetPayments() throws Exception {
        String token = createToken("read");

        mockMvc.perform(get("/api/v1/payments")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void readScopeTokenCanAccessGetPaymentById() throws Exception {
        String token = createToken("read");

        mockMvc.perform(get("/api/v1/payments/999")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void readScopeTokenCannotPostPaymentsReturns403WithJsonBody() throws Exception {
        String token = createToken("read");

        mockMvc.perform(post("/api/v1/payments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100.00, \"paymentMethod\": \"PIX\"}"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.detail").value("Insufficient scope for this operation"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    // endregion

    // region: write scope enforcement (write implies read)

    @Test
    void writeScopeTokenCanPostPayments() throws Exception {
        String token = createToken("write read");

        mockMvc.perform(post("/api/v1/payments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100.00, \"paymentMethod\": \"PIX\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void writeScopeTokenCanGetPaymentsDueToImpliedRead() throws Exception {
        String token = createToken("write read");

        mockMvc.perform(get("/api/v1/payments")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    // endregion

    // region: client:admin scope enforcement

    @Test
    void clientAdminScopeTokenCanAccessGetClients() throws Exception {
        String token = createToken("client:admin");

        mockMvc.perform(get("/api/v1/clients")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void readScopeTokenCannotAccessClientsReturns403() throws Exception {
        String token = createToken("read");

        mockMvc.perform(get("/api/v1/clients")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.detail").value("Insufficient scope for this operation"));
    }

    @Test
    void writeScopeTokenCannotAccessClientsReturns403() throws Exception {
        String token = createToken("write read");

        mockMvc.perform(get("/api/v1/clients")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.detail").value("Insufficient scope for this operation"));
    }

    // endregion

    // region: invalid JWT handling → 401

    @Test
    void expiredJwtReturns401() throws Exception {
        String expiredToken = createExpiredToken("read");

        mockMvc.perform(get("/api/v1/payments")
                .header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                .header("Authorization", "Bearer not.a.valid.jwt.token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongSignatureJwtReturns401() throws Exception {
        String wrongSigToken = createTokenWithWrongSignature("read");

        mockMvc.perform(get("/api/v1/payments")
                .header("Authorization", "Bearer " + wrongSigToken))
            .andExpect(status().isUnauthorized());
    }

    // endregion

    // region: public paths accessible without token

    @Test
    void swaggerUiIndexIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }

    @Test
    void h2ConsoleIsAccessibleWithoutToken() throws Exception {
        // H2 console path must NOT be blocked by security (not 401 or 403).
        // The actual servlet response (200/302/500) depends on the test environment.
        mockMvc.perform(get("/h2-console/"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                org.assertj.core.api.Assertions.assertThat(status)
                    .as("H2 console should not be blocked by security (not 401 or 403)")
                    .isNotEqualTo(401)
                    .isNotEqualTo(403);
            });
    }

    @Test
    void oauth2TokenPathIsPermittedWithoutToken() throws Exception {
        // No token endpoint handler yet (Task 3); security must NOT block this path
        mockMvc.perform(post("/oauth2/token"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                org.assertj.core.api.Assertions.assertThat(status)
                    .isNotEqualTo(401)
                    .isNotEqualTo(403);
            });
    }

    // endregion
}
