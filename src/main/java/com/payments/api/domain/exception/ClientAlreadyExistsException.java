package com.payments.api.domain.exception;

public class ClientAlreadyExistsException extends RuntimeException {

    public ClientAlreadyExistsException(String message) {
        super(message);
    }
}
