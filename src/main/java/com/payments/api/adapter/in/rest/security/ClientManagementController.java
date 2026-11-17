package com.payments.api.adapter.in.rest.security;

import com.payments.api.adapter.in.rest.security.dto.ClientDetailResponse;
import com.payments.api.adapter.in.rest.security.dto.ClientRegistrationRequest;
import com.payments.api.adapter.in.rest.security.dto.ClientRegistrationResponse;
import com.payments.api.adapter.in.rest.security.dto.SecretRotationResponse;
import com.payments.api.application.port.in.security.ClientSecretResult;
import com.payments.api.application.port.in.security.ManageClientUseCase;
import com.payments.api.domain.exception.ClientNotFoundException;
import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientManagementController {

    private final ManageClientUseCase manageClientUseCase;

    public ClientManagementController(ManageClientUseCase manageClientUseCase) {
        this.manageClientUseCase = manageClientUseCase;
    }

    @PostMapping
    public ResponseEntity<ClientRegistrationResponse> registerClient(
            @Valid @RequestBody ClientRegistrationRequest request) {

        ClientScope scope = ClientScope.fromString(request.getScope());

        OAuthClient client = manageClientUseCase.registerClient(request.getClientName(), scope);

        ClientRegistrationResponse response = new ClientRegistrationResponse(
                client.getClientId(),
                client.getClientName(),
                client.getRawSecret(),
                request.getScope(),
                client.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClientDetailResponse>> listClients() {
        List<ClientDetailResponse> clients = manageClientUseCase.listClients().stream()
                .map(this::toDetailResponse)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientDetailResponse> getClient(@PathVariable String clientId) {
        return manageClientUseCase.getClient(clientId)
                .map(this::toDetailResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));
    }

    @PostMapping("/{clientId}/rotate-secret")
    public ResponseEntity<SecretRotationResponse> rotateSecret(@PathVariable String clientId) {
        ClientSecretResult result = manageClientUseCase.rotateSecret(clientId);

        SecretRotationResponse response = new SecretRotationResponse(
                result.clientId(),
                result.clientName(),
                result.rawSecret(),
                result.rotatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> revokeClient(@PathVariable String clientId) {
        manageClientUseCase.revokeClient(clientId);
        return ResponseEntity.noContent().build();
    }

    private ClientDetailResponse toDetailResponse(OAuthClient client) {
        return new ClientDetailResponse(
                client.getClientId(),
                client.getClientName(),
                client.getScope().toScopeString(),
                client.isActive(),
                client.getCreatedAt(),
                client.getLastUsedAt()
        );
    }
}
