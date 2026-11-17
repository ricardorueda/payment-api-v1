package com.payments.api.adapter.out.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthClientJpaRepository extends JpaRepository<OAuthClientEntity, String> {

    Optional<OAuthClientEntity> findByClientId(String clientId);

    Optional<OAuthClientEntity> findByClientName(String clientName);

    boolean existsByClientId(String clientId);

    boolean existsByClientName(String clientName);
}
