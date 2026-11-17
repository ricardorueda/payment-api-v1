package com.payments.api.adapter.in.rest.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.payments.api.adapter.in.rest.security.dto.ClientRegistrationRequest;
import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "jwt.secret-key=test-secret-key-for-client-mgmt-integration-tests-hmac-sha256",
    "jwt.issuer=payments-api",
    "jwt.expiration-seconds=1800",
    "bootstrap.client-id=00000000-0000-0000-0000-000000000001",
    "bootstrap.client-name=admin-client",
    "bootstrap.client-secret=admin-secret-test"
})
@AutoConfigureMockMvc
class ClientManagementControllerIntegrationTest {

    private static final String TEST_SECRET =
        "test-secret-key-for-client-mgmt-integration-tests-hmac-sha256";
    private static final String BOOTSTRAP_ADMIN_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepositoryPort clientRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUpTestClients() {
        // Remove any test-registered clients (bootstrap admin stays via ClientBootstrapConfig)
        clientRepository.findAll().stream()
            .filter(c -> !BOOTSTRAP_ADMIN_ID.equals(c.getClientId()))
            .forEach(c -> clientRepository.deleteByClientId(c.getClientId()));
    }

    private String createAdminToken() throws Exception {
        return buildJwt("client:admin");
    }

    private String createReadToken() throws Exception {
        return buildJwt("read");
    }

    private String createWriteToken() throws Exception {
        return buildJwt("write read");
    }

    private String buildJwt(String scope) throws Exception {
        byte[] keyBytes = TEST_SECRET.getBytes(StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(keyBytes);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject("test-admin-client-id")
            .issuer("payments-api")
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 30 * 60 * 1000))
            .claim("scope", scope)
            .claim("client_name", "test-admin-service")
            .jwtID(UUID.randomUUID().toString())
            .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(signer);
        return jwt.serialize();
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // region: POST /api/v1/clients — auth enforcement

    @Test
    void registerClientWithoutAuthReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("new-service", "read"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registerClientWithReadScopeTokenReturns403() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createReadToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("new-service", "read"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void registerClientWithWriteScopeTokenReturns403() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createWriteToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("new-service", "read"))))
            .andExpect(status().isForbidden());
    }

    // endregion

    // region: POST /api/v1/clients — success

    @Test
    void registerClientWithAdminTokenAndValidBodyReturns201WithClientIdSecretScopeAndCreatedAt()
            throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("order-service", "read"))))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.clientId").exists())
            .andExpect(jsonPath("$.clientName").value("order-service"))
            .andExpect(jsonPath("$.clientSecret").exists())
            .andExpect(jsonPath("$.scope").value("read"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void registerClientWithWriteScopeInBodyReturns201WithWriteScope() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("write-service", "write"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.scope").value("write"));
    }

    // endregion

    // region: POST /api/v1/clients — error cases

    @Test
    void registerClientWithClientAdminScopeInBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("admin-attempt", "client:admin"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerClientWithDuplicateNameReturns409() throws Exception {
        // First registration
        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("duplicate-service", "read"))))
            .andExpect(status().isCreated());

        // Second registration with same name
        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("duplicate-service", "read"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Client name already exists"));
    }

    @Test
    void registerClientWithMissingClientNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scope\":\"read\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerClientWithInvalidScopeStringReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("service", "invalid-scope"))))
            .andExpect(status().isBadRequest());
    }

    // endregion

    // region: GET /api/v1/clients

    @Test
    void listClientsWithAdminTokenReturns200WithArray() throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listClientsWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
            .andExpect(status().isUnauthorized());
    }

    // endregion

    // region: GET /api/v1/clients/{clientId}

    @Test
    void getClientWithAdminTokenAndExistingClientReturns200() throws Exception {
        // Register a client first
        MvcResult registerResult = mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("get-test-service", "read"))))
            .andExpect(status().isCreated())
            .andReturn();

        String clientId = com.jayway.jsonpath.JsonPath.read(
            registerResult.getResponse().getContentAsString(), "$.clientId");

        mockMvc.perform(get("/api/v1/clients/" + clientId)
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientId").value(clientId))
            .andExpect(jsonPath("$.clientName").value("get-test-service"))
            .andExpect(jsonPath("$.scope").exists())
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getClientWithUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/clients/nonexistent-client-id-that-does-not-exist")
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isNotFound());
    }

    // endregion

    // region: POST /api/v1/clients/{clientId}/rotate-secret

    @Test
    void rotateSecretWithAdminTokenReturns200WithNewSecret() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("rotate-test-service", "read"))))
            .andExpect(status().isCreated())
            .andReturn();

        String clientId = com.jayway.jsonpath.JsonPath.read(
            registerResult.getResponse().getContentAsString(), "$.clientId");
        String originalSecret = com.jayway.jsonpath.JsonPath.read(
            registerResult.getResponse().getContentAsString(), "$.clientSecret");

        MvcResult rotateResult = mockMvc.perform(post("/api/v1/clients/" + clientId + "/rotate-secret")
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientId").value(clientId))
            .andExpect(jsonPath("$.clientSecret").exists())
            .andExpect(jsonPath("$.rotatedAt").exists())
            .andReturn();

        String newSecret = com.jayway.jsonpath.JsonPath.read(
            rotateResult.getResponse().getContentAsString(), "$.clientSecret");

        assertThat(newSecret).isNotEqualTo(originalSecret);
    }

    @Test
    void rotateSecretForNonexistentClientReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/clients/nonexistent-id/rotate-secret")
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isNotFound());
    }

    // endregion

    // region: DELETE /api/v1/clients/{clientId}

    @Test
    void deleteClientWithAdminTokenReturns204() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("delete-test-service", "read"))))
            .andExpect(status().isCreated())
            .andReturn();

        String clientId = com.jayway.jsonpath.JsonPath.read(
            registerResult.getResponse().getContentAsString(), "$.clientId");

        mockMvc.perform(delete("/api/v1/clients/" + clientId)
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteBootstrapAdminClientReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/" + BOOTSTRAP_ADMIN_ID)
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteNonexistentClientReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/nonexistent-client-to-delete")
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isNotFound());
    }

    // endregion

    // region: full CRUD lifecycle

    @Test
    void fullCrudLifecycle() throws Exception {
        // 1. Register
        MvcResult registerResult = mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new ClientRegistrationRequest("lifecycle-service", "write"))))
            .andExpect(status().isCreated())
            .andReturn();

        String clientId = com.jayway.jsonpath.JsonPath.read(
            registerResult.getResponse().getContentAsString(), "$.clientId");

        // 2. List — verify present
        mockMvc.perform(get("/api/v1/clients")
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.clientId == '" + clientId + "')]").exists());

        // 3. Get
        mockMvc.perform(get("/api/v1/clients/" + clientId)
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientId").value(clientId));

        // 4. Rotate secret
        mockMvc.perform(post("/api/v1/clients/" + clientId + "/rotate-secret")
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientSecret").exists());

        // 5. Delete
        mockMvc.perform(delete("/api/v1/clients/" + clientId)
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isNoContent());

        // 6. Get after delete — verify 404
        mockMvc.perform(get("/api/v1/clients/" + clientId)
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isNotFound());
    }

    // endregion

    // region: bootstrap admin client accessible after startup

    @Test
    void bootstrapAdminClientIsAccessibleAndAuthenticatableAfterStartup() throws Exception {
        // The bootstrap admin should exist and be retrievable
        mockMvc.perform(get("/api/v1/clients/" + BOOTSTRAP_ADMIN_ID)
                .header("Authorization", "Bearer " + createAdminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientId").value(BOOTSTRAP_ADMIN_ID))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void bootstrapAdminClientCanAuthenticateViaTokenEndpoint() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", BOOTSTRAP_ADMIN_ID)
                .param("client_secret", "admin-secret-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.scope").value("client:admin"));
    }

    // endregion
}
