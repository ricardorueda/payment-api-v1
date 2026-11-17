package com.payments.api.adapter.in.rest.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenErrorResponse {

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_description")
    private String errorDescription;

    public TokenErrorResponse() {
    }

    public TokenErrorResponse(String error, String errorDescription) {
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}
