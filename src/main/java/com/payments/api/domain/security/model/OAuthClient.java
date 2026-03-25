package com.payments.api.domain.security.model;

import com.payments.api.domain.security.valueobject.ClientScope;

import java.time.LocalDateTime;
import java.util.Objects;

public class OAuthClient {

    private String clientId;
    private String clientName;
    private String hashedSecret;
    private ClientScope scope;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    // Transient field — populated only during registration/rotation, never persisted
    private transient String rawSecret;

    public OAuthClient() {
    }

    public OAuthClient(String clientId, String clientName, String hashedSecret, ClientScope scope,
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

    public String getRawSecret() {
        return rawSecret;
    }

    public void setRawSecret(String rawSecret) {
        this.rawSecret = rawSecret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OAuthClient that = (OAuthClient) o;
        return Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
