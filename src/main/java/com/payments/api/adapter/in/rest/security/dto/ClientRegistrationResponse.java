package com.payments.api.adapter.in.rest.security.dto;

import java.time.LocalDateTime;

public class ClientRegistrationResponse {

    private String clientId;
    private String clientName;
    private String clientSecret;
    private String scope;
    private LocalDateTime createdAt;

    public ClientRegistrationResponse() {
    }

    public ClientRegistrationResponse(String clientId, String clientName, String clientSecret,
                                      String scope, LocalDateTime createdAt) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.createdAt = createdAt;
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

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
