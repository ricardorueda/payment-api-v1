package com.payments.api.application.port.out.security;

import com.payments.api.domain.security.model.OAuthClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClientRepositoryPort {

    OAuthClient save(OAuthClient client);

    Optional<OAuthClient> findByClientId(String clientId);

    Optional<OAuthClient> findByClientName(String clientName);

    List<OAuthClient> findAll();

    void deleteByClientId(String clientId);

    boolean existsByClientId(String clientId);

    boolean existsByClientName(String clientName);

    void updateLastUsedAt(String clientId, LocalDateTime timestamp);
}
