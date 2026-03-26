package com.payments.api.application.service.security;

import com.payments.api.application.port.in.security.ClientSecretResult;
import com.payments.api.application.port.in.security.ManageClientUseCase;
import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.domain.exception.ClientAlreadyExistsException;
import com.payments.api.domain.exception.ClientNotFoundException;
import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClientManagementService implements ManageClientUseCase {

    private final ClientRepositoryPort clientRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String bootstrapAdminClientId;

    public ClientManagementService(ClientRepositoryPort clientRepository,
                                   BCryptPasswordEncoder passwordEncoder,
                                   String bootstrapAdminClientId) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminClientId = bootstrapAdminClientId;
    }

    @Override
    public OAuthClient registerClient(String clientName, ClientScope scope) {
        if (scope == ClientScope.CLIENT_ADMIN) {
            throw new IllegalArgumentException("Cannot self-assign client:admin scope during registration");
        }

        if (clientRepository.existsByClientName(clientName)) {
            throw new ClientAlreadyExistsException("Client name already exists: " + clientName);
        }

        String rawSecret = generateRawSecret();
        String hashedSecret = passwordEncoder.encode(rawSecret);
        String clientId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        OAuthClient client = new OAuthClient(clientId, clientName, hashedSecret, scope, true, now, null);
        client.setRawSecret(rawSecret);

        OAuthClient saved = clientRepository.save(client);
        saved.setRawSecret(rawSecret);
        return saved;
    }

    @Override
    public List<OAuthClient> listClients() {
        return clientRepository.findAll();
    }

    @Override
    public Optional<OAuthClient> getClient(String clientId) {
        return clientRepository.findByClientId(clientId);
    }

    @Override
    public ClientSecretResult rotateSecret(String clientId) {
        OAuthClient client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        if (!client.isActive()) {
            throw new IllegalStateException("Cannot rotate secret for inactive client: " + clientId);
        }

        String rawSecret = generateRawSecret();
        String hashedSecret = passwordEncoder.encode(rawSecret);
        client.setHashedSecret(hashedSecret);
        clientRepository.save(client);

        LocalDateTime rotatedAt = LocalDateTime.now();
        return new ClientSecretResult(client.getClientId(), client.getClientName(), rawSecret, rotatedAt);
    }

    @Override
    public void revokeClient(String clientId) {
        if (clientId.equals(bootstrapAdminClientId)) {
            throw new IllegalArgumentException("Cannot delete the bootstrap admin client");
        }

        if (!clientRepository.existsByClientId(clientId)) {
            throw new ClientNotFoundException("Client not found: " + clientId);
        }

        clientRepository.deleteByClientId(clientId);
    }

    private String generateRawSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
