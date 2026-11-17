package com.payments.api.adapter.in.rest.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
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
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
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
class TokenControllerIntegrationTest {

    private static final String TEST_SECRET =
        "test-secret-key-for-integration-tests-hmac-sha256-at-least-32-bytes";

    private static final String ACTIVE_CLIENT_ID = "aa111111-1111-1111-1111-111111111111";
    private static final String ACTIVE_CLIENT_NAME = "integration-test-active-service";
    private static final String ACTIVE_CLIENT_SECRET = "integration-test-plain-secret-active";

    private static final String INACTIVE_CLIENT_ID = "bb222222-2222-2222-2222-222222222222";
    private static final String INACTIVE_CLIENT_NAME = "integration-test-inactive-service";
    private static final String INACTIVE_CLIENT_SECRET = "integration-test-plain-secret-inactive";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepositoryPort clientRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        OAuthClient activeClient = new OAuthClient(
            ACTIVE_CLIENT_ID, ACTIVE_CLIENT_NAME,
            passwordEncoder.encode(ACTIVE_CLIENT_SECRET),
            ClientScope.WRITE, true,
            LocalDateTime.now(), null
        );
        clientRepository.save(activeClient);

        OAuthClient inactiveClient = new OAuthClient(
            INACTIVE_CLIENT_ID, INACTIVE_CLIENT_NAME,
            passwordEncoder.encode(INACTIVE_CLIENT_SECRET),
            ClientScope.READ, false,
            LocalDateTime.now(), null
        );
        clientRepository.save(inactiveClient);
    }

    private String basicAuth(String clientId, String secret) {
        String credentials = clientId + ":" + secret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validBasicAuthCredentialsReturns200WithTokenResponse() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", basicAuth(ACTIVE_CLIENT_ID, ACTIVE_CLIENT_SECRET))
                .param("grant_type", "client_credentials"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").value(1800))
            .andExpect(jsonPath("$.scope").exists());
    }

    @Test
    void validFormBodyCredentialsReturns200WithTokenResponse() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", ACTIVE_CLIENT_ID)
                .param("client_secret", ACTIVE_CLIENT_SECRET))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").value(1800))
            .andExpect(jsonPath("$.scope").exists());
    }

    @Test
    void invalidClientIdReturns401WithInvalidClientError() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", "nonexistent-client-id")
                .param("client_secret", "some-secret"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("invalid_client"))
            .andExpect(jsonPath("$.error_description").value("Invalid client credentials"));
    }

    @Test
    void wrongClientSecretReturns401WithInvalidClientError() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", ACTIVE_CLIENT_ID)
                .param("client_secret", "wrong-secret"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("invalid_client"))
            .andExpect(jsonPath("$.error_description").value("Invalid client credentials"));
    }

    @Test
    void inactiveClientCredentialsReturn401WithInvalidClientError() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", INACTIVE_CLIENT_ID)
                .param("client_secret", INACTIVE_CLIENT_SECRET))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("invalid_client"))
            .andExpect(jsonPath("$.error_description").value("Invalid client credentials"));
    }

    @Test
    void missingGrantTypeReturns400WithUnsupportedGrantTypeError() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("client_id", ACTIVE_CLIENT_ID)
                .param("client_secret", ACTIVE_CLIENT_SECRET))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("unsupported_grant_type"))
            .andExpect(jsonPath("$.error_description").value(
                "Only client_credentials grant type is supported"));
    }

    @Test
    void unsupportedGrantTypeReturns400WithUnsupportedGrantTypeError() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("client_id", ACTIVE_CLIENT_ID)
                .param("client_secret", ACTIVE_CLIENT_SECRET))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("unsupported_grant_type"))
            .andExpect(jsonPath("$.error_description").value(
                "Only client_credentials grant type is supported"));
    }

    @Test
    void noCredentialsAtAllReturns400WithInvalidRequestError() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("invalid_request"))
            .andExpect(jsonPath("$.error_description").value("Missing client credentials"));
    }

    @Test
    void responseContentTypeIsApplicationJsonForSuccessResponse() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", ACTIVE_CLIENT_ID)
                .param("client_secret", ACTIVE_CLIENT_SECRET))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void responseContentTypeIsApplicationJsonForErrorResponse() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", "nonexistent")
                .param("client_secret", "secret"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void issuedJwtIsDecodableAndValidatesAgainstConfiguredHmacSecret() throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", ACTIVE_CLIENT_ID)
                .param("client_secret", ACTIVE_CLIENT_SECRET))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.read(responseBody, "$.access_token");

        SignedJWT signedJWT = SignedJWT.parse(accessToken);
        assertThat(signedJWT.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);

        byte[] keyBytes = TEST_SECRET.getBytes(StandardCharsets.UTF_8);
        MACVerifier verifier = new MACVerifier(keyBytes);
        assertThat(signedJWT.verify(verifier)).isTrue();

        assertThat(signedJWT.getJWTClaimsSet().getSubject()).isEqualTo(ACTIVE_CLIENT_ID);
        assertThat(signedJWT.getJWTClaimsSet().getIssuer()).isEqualTo("payments-api");
        assertThat(signedJWT.getJWTClaimsSet().getStringClaim("scope"))
            .isEqualTo(ClientScope.WRITE.toScopeString());
        assertThat(signedJWT.getJWTClaimsSet().getJWTID()).isNotNull();
    }
}
