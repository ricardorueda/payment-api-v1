package com.payments.api.adapter.in.rest.security.dto;

import java.time.LocalDateTime;

public class SecretRotationResponse {

    private String clientId;
    private String clientName;
    private String clientSecret;
    private LocalDateTime rotatedAt;

    public SecretRotationResponse() {
    }

    public SecretRotationResponse(String clientId, String clientName, String clientSecret,
                                  LocalDateTime rotatedAt) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientSecret = clientSecret;
        this.rotatedAt = rotatedAt;
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

    public LocalDateTime getRotatedAt() {
        return rotatedAt;
    }

    public void setRotatedAt(LocalDateTime rotatedAt) {
        this.rotatedAt = rotatedAt;
    }
}
