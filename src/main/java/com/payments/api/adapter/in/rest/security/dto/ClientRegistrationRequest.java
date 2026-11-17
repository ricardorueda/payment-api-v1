package com.payments.api.adapter.in.rest.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClientRegistrationRequest {

    @NotBlank(message = "clientName is required")
    @Size(max = 100, message = "clientName must be at most 100 characters")
    private String clientName;

    @NotBlank(message = "scope is required")
    private String scope;

    public ClientRegistrationRequest() {
    }

    public ClientRegistrationRequest(String clientName, String scope) {
        this.clientName = clientName;
        this.scope = scope;
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
}
