package com.payments.api.application.port.in.security;

import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;

import java.util.List;
import java.util.Optional;

public interface ManageClientUseCase {

    OAuthClient registerClient(String clientName, ClientScope scope);

    List<OAuthClient> listClients();

    Optional<OAuthClient> getClient(String clientId);

    ClientSecretResult rotateSecret(String clientId);

    void revokeClient(String clientId);
}
