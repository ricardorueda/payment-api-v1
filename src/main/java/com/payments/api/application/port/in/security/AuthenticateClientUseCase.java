package com.payments.api.application.port.in.security;

public interface AuthenticateClientUseCase {

    TokenResult authenticate(String clientId, String clientSecret);
}
