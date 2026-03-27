package com.payments.api.domain.security.valueobject;

public enum ClientScope {
    READ("read"),
    WRITE("write read"),
    CLIENT_ADMIN("client:admin");

    private final String scopeString;

    ClientScope(String scopeString) {
        this.scopeString = scopeString;
    }

    public String toScopeString() {
        return scopeString;
    }

    public static ClientScope fromString(String scope) {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("Invalid scope: " + scope);
        }
        return switch (scope.toLowerCase()) {
            case "read" -> READ;
            case "write" -> WRITE;
            case "client:admin" -> CLIENT_ADMIN;
            default -> throw new IllegalArgumentException("Invalid scope: " + scope);
        };
    }
}
