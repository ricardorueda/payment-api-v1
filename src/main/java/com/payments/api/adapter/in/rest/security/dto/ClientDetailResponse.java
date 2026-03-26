package com.payments.api.adapter.in.rest.security.dto;

import java.time.LocalDateTime;

public class ClientDetailResponse {

    private String clientId;
    private String clientName;
    private String scope;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public ClientDetailResponse() {
    }

    public ClientDetailResponse(String clientId, String clientName, String scope,
                                boolean active, LocalDateTime createdAt, LocalDateTime lastUsedAt) {
        this.clientId = clientId;
        this.clientName = clientName;
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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
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
