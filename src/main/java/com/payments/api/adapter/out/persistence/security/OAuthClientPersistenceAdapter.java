package com.payments.api.adapter.out.persistence.security;

import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.domain.security.model.OAuthClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OAuthClientPersistenceAdapter implements ClientRepositoryPort {

    private final OAuthClientJpaRepository jpaRepository;

    public OAuthClientPersistenceAdapter(OAuthClientJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OAuthClient save(OAuthClient client) {
        OAuthClientEntity entity = toEntity(client);
        OAuthClientEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<OAuthClient> findByClientId(String clientId) {
        return jpaRepository.findByClientId(clientId).map(this::toDomain);
    }

    @Override
    public Optional<OAuthClient> findByClientName(String clientName) {
        return jpaRepository.findByClientName(clientName).map(this::toDomain);
    }

    @Override
    public List<OAuthClient> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByClientId(String clientId) {
        jpaRepository.deleteById(clientId);
    }

    @Override
    public boolean existsByClientId(String clientId) {
        return jpaRepository.existsByClientId(clientId);
    }

    @Override
    public boolean existsByClientName(String clientName) {
        return jpaRepository.existsByClientName(clientName);
    }

    @Override
    public void updateLastUsedAt(String clientId, LocalDateTime timestamp) {
        jpaRepository.findByClientId(clientId).ifPresent(entity -> {
            entity.setLastUsedAt(timestamp);
            jpaRepository.save(entity);
        });
    }

    private OAuthClientEntity toEntity(OAuthClient client) {
        return new OAuthClientEntity(
                client.getClientId(),
                client.getClientName(),
                client.getHashedSecret(),
                client.getScope(),
                client.isActive(),
                client.getCreatedAt(),
                client.getLastUsedAt()
        );
    }

    private OAuthClient toDomain(OAuthClientEntity entity) {
        return new OAuthClient(
                entity.getClientId(),
                entity.getClientName(),
                entity.getHashedSecret(),
                entity.getScope(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getLastUsedAt()
        );
    }
}
