package com.payments.api.config;

import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "jwt.secret-key=test-secret-key-for-bootstrap-integration-tests-hmac-sha256",
    "jwt.issuer=payments-api",
    "jwt.expiration-seconds=1800",
    "bootstrap.client-id=00000000-0000-0000-0000-000000000001",
    "bootstrap.client-name=admin-client",
    "bootstrap.client-secret=admin-bootstrap-secret-test",
    "spring.datasource.url=jdbc:h2:mem:bootstrapinttest;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class ClientBootstrapIntegrationTest {

    private static final String BOOTSTRAP_CLIENT_ID = "00000000-0000-0000-0000-000000000001";
    private static final String BOOTSTRAP_CLIENT_SECRET = "admin-bootstrap-secret-test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepositoryPort clientRepository;

    @Autowired
    private ClientBootstrapConfig clientBootstrapConfig;

    @Test
    void bootstrapAdminClientExistsAfterApplicationStartup() {
        Optional<OAuthClient> adminClient = clientRepository.findByClientId(BOOTSTRAP_CLIENT_ID);

        assertThat(adminClient).isPresent();
    }

    @Test
    void bootstrapAdminClientHasClientAdminScope() {
        OAuthClient adminClient = clientRepository.findByClientId(BOOTSTRAP_CLIENT_ID)
                .orElseThrow(() -> new AssertionError("Bootstrap admin client not found"));

        assertThat(adminClient.getScope()).isEqualTo(ClientScope.CLIENT_ADMIN);
    }

    @Test
    void bootstrapAdminClientIsActiveAfterStartup() {
        OAuthClient adminClient = clientRepository.findByClientId(BOOTSTRAP_CLIENT_ID)
                .orElseThrow(() -> new AssertionError("Bootstrap admin client not found"));

        assertThat(adminClient.isActive()).isTrue();
    }

    @Test
    void bootstrapAdminClientCanAuthenticateAndReceiveJwtWithClientAdminScope() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", BOOTSTRAP_CLIENT_ID)
                .param("client_secret", BOOTSTRAP_CLIENT_SECRET))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.scope").value("client:admin"));
    }

    @Test
    void rerunningBootstrapConfigDoesNotCreateDuplicateAndDoesNotThrow() throws Exception {
        long countBefore = clientRepository.findAll().stream()
                .filter(c -> BOOTSTRAP_CLIENT_ID.equals(c.getClientId()))
                .count();

        assertThatNoException().isThrownBy(() -> clientBootstrapConfig.run(null));

        long countAfter = clientRepository.findAll().stream()
                .filter(c -> BOOTSTRAP_CLIENT_ID.equals(c.getClientId()))
                .count();

        assertThat(countAfter).isEqualTo(countBefore);
    }
}
