package com.payments.api.adapter.out.persistence.security;

import com.payments.api.domain.security.valueobject.ClientScope;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_clients")
public class OAuthClientEntity {

    @Id
    @Column(name = "client_id", length = 36)
    private String clientId;

    @Column(name = "client_name", unique = true, nullable = false, length = 100)
    private String clientName;

    @Column(name = "hashed_secret", nullable = false, length = 72)
    private String hashedSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private ClientScope scope;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public OAuthClientEntity() {
    }

    public OAuthClientEntity(String clientId, String clientName, String hashedSecret, ClientScope scope,
                              boolean active, LocalDateTime createdAt, LocalDateTime lastUsedAt) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.hashedSecret = hashedSecret;
        this.scope = scope;
        this.active = active;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getHashedSecret() {
        return hashedSecret;
    }

    public void setHashedSecret(String hashedSecret) {
        this.hashedSecret = hashedSecret;
    }

    public ClientScope getScope() {
        return scope;
    }

    public void setScope(ClientScope scope) {
        this.scope = scope;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
