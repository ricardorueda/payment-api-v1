package com.payments.api.application.port.in.security;

public record TokenResult(
    String accessToken,
    String tokenType,
    long expiresIn,
    String scope
) {}
