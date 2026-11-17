package com.payments.api.application.port.in.security;

import java.time.LocalDateTime;

public record ClientSecretResult(
        String clientId,
        String clientName,
        String rawSecret,
        LocalDateTime rotatedAt
) {
}
