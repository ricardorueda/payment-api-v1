package com.payments.api.application.service.security;

import com.payments.api.application.port.in.security.ClientSecretResult;
import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.domain.exception.ClientAlreadyExistsException;
import com.payments.api.domain.exception.ClientNotFoundException;
import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientManagementServiceTest {

    @Mock
    private ClientRepositoryPort clientRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private ClientManagementService service;

    private static final String BOOTSTRAP_ADMIN_ID = "00000000-0000-0000-0000-000000000001";
    private static final String CLIENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String CLIENT_NAME = "order-service";
    private static final String HASHED_SECRET = "$2a$10$hashedValue";

    @BeforeEach
    void setUp() {
        service = new ClientManagementService(clientRepository, passwordEncoder, BOOTSTRAP_ADMIN_ID);
    }

    // region: registerClient

    @Test
    void registerClientWithReadScopeReturnsClientWithNonNullRawSecret() {
        when(clientRepository.existsByClientName(CLIENT_NAME)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OAuthClient result = service.registerClient(CLIENT_NAME, ClientScope.READ);

        assertThat(result.getRawSecret()).isNotNull();
        assertThat(result.getRawSecret()).isNotBlank();
    }

    @Test
    void registerClientWithWriteScopeReturnsClientWithWriteScope() {
        when(clientRepository.existsByClientName(CLIENT_NAME)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OAuthClient result = service.registerClient(CLIENT_NAME, ClientScope.WRITE);

        assertThat(result.getScope()).isEqualTo(ClientScope.WRITE);
    }

    @Test
    void registerClientWithReadScopeReturnsClientWithCorrectName() {
        when(clientRepository.existsByClientName(CLIENT_NAME)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OAuthClient result = service.registerClient(CLIENT_NAME, ClientScope.READ);

        assertThat(result.getClientName()).isEqualTo(CLIENT_NAME);
    }

    @Test
    void registerClientWithReadScopeGeneratesUuidClientId() {
        when(clientRepository.existsByClientName(CLIENT_NAME)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OAuthClient result = service.registerClient(CLIENT_NAME, ClientScope.READ);

        assertThat(result.getClientId()).isNotNull();
        assertThat(result.getClientId()).hasSize(36);
    }

    @Test
    void registerClientWithDuplicateNameThrowsClientAlreadyExistsException() {
        when(clientRepository.existsByClientName(CLIENT_NAME)).thenReturn(true);

        assertThatThrownBy(() -> service.registerClient(CLIENT_NAME, ClientScope.READ))
                .isInstanceOf(ClientAlreadyExistsException.class);
    }

    @Test
    void registerClientWithClientAdminScopeThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.registerClient(CLIENT_NAME, ClientScope.CLIENT_ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerClientWithClientAdminScopeDoesNotCheckNameUniqueness() {
        assertThatThrownBy(() -> service.registerClient(CLIENT_NAME, ClientScope.CLIENT_ADMIN))
                .isInstanceOf(IllegalArgumentException.class);

        verify(clientRepository, never()).existsByClientName(any());
    }

    @Test
    void registerClientEncodesBCryptHashBeforePersisting() {
        when(clientRepository.existsByClientName(CLIENT_NAME)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registerClient(CLIENT_NAME, ClientScope.READ);

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().getHashedSecret()).isEqualTo(HASHED_SECRET);
    }

    // endregion

    // region: listClients

    @Test
    void listClientsDelegatesToRepositoryFindAll() {
        OAuthClient client1 = new OAuthClient(CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
                ClientScope.READ, true, LocalDateTime.now(), null);
        OAuthClient client2 = new OAuthClient("other-id", "other-service", HASHED_SECRET,
                ClientScope.WRITE, true, LocalDateTime.now(), null);
        when(clientRepository.findAll()).thenReturn(List.of(client1, client2));

        List<OAuthClient> result = service.listClients();

        assertThat(result).hasSize(2);
        verify(clientRepository).findAll();
    }

    @Test
    void listClientsReturnsEmptyListWhenNoClients() {
        when(clientRepository.findAll()).thenReturn(List.of());

        List<OAuthClient> result = service.listClients();

        assertThat(result).isEmpty();
    }

    // endregion

    // region: getClient

    @Test
    void getClientWithKnownIdReturnsOptionalContainingClient() {
        OAuthClient client = new OAuthClient(CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
                ClientScope.READ, true, LocalDateTime.now(), null);
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client));

        Optional<OAuthClient> result = service.getClient(CLIENT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getClientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    void getClientWithUnknownIdReturnsEmptyOptional() {
        when(clientRepository.findByClientId("unknown-id")).thenReturn(Optional.empty());

        Optional<OAuthClient> result = service.getClient("unknown-id");

        assertThat(result).isEmpty();
    }

    // endregion

    // region: rotateSecret

    @Test
    void rotateSecretForExistingClientUpdatesHashedSecretInRepository() {
        OAuthClient client = new OAuthClient(CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
                ClientScope.READ, true, LocalDateTime.now(), null);
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$newHashedValue");
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClientSecretResult result = service.rotateSecret(CLIENT_ID);

        assertThat(result.rawSecret()).isNotNull();
        assertThat(result.rawSecret()).isNotBlank();
        verify(clientRepository).save(argThat(c -> "$2a$10$newHashedValue".equals(c.getHashedSecret())));
    }

    @Test
    void rotateSecretForExistingClientReturnsCorrectClientIdAndName() {
        OAuthClient client = new OAuthClient(CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
                ClientScope.READ, true, LocalDateTime.now(), null);
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$newHashedValue");
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClientSecretResult result = service.rotateSecret(CLIENT_ID);

        assertThat(result.clientId()).isEqualTo(CLIENT_ID);
        assertThat(result.clientName()).isEqualTo(CLIENT_NAME);
        assertThat(result.rotatedAt()).isNotNull();
    }

    @Test
    void rotateSecretForNonexistentClientThrowsClientNotFoundException() {
        when(clientRepository.findByClientId("nonexistent-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotateSecret("nonexistent-id"))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void rotateSecretForInactiveClientThrowsIllegalStateException() {
        OAuthClient inactiveClient = new OAuthClient(CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
                ClientScope.READ, false, LocalDateTime.now(), null);
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(inactiveClient));

        assertThatThrownBy(() -> service.rotateSecret(CLIENT_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    // endregion

    // region: revokeClient

    @Test
    void revokeClientForExistingNonAdminClientCallsDeleteByClientId() {
        when(clientRepository.existsByClientId(CLIENT_ID)).thenReturn(true);

        service.revokeClient(CLIENT_ID);

        verify(clientRepository).deleteByClientId(CLIENT_ID);
    }

    @Test
    void revokeClientForBootstrapAdminClientThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> service.revokeClient(BOOTSTRAP_ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revokeClientForBootstrapAdminClientDoesNotCallDeleteByClientId() {
        assertThatThrownBy(() -> service.revokeClient(BOOTSTRAP_ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verify(clientRepository, never()).deleteByClientId(any());
    }

    @Test
    void revokeClientForNonexistentClientThrowsClientNotFoundException() {
        when(clientRepository.existsByClientId("nonexistent-id")).thenReturn(false);

        assertThatThrownBy(() -> service.revokeClient("nonexistent-id"))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void revokeClientForNonexistentClientDoesNotCallDeleteByClientId() {
        when(clientRepository.existsByClientId("nonexistent-id")).thenReturn(false);

        assertThatThrownBy(() -> service.revokeClient("nonexistent-id"))
                .isInstanceOf(ClientNotFoundException.class);

        verify(clientRepository, never()).deleteByClientId(any());
    }

    // endregion
}
