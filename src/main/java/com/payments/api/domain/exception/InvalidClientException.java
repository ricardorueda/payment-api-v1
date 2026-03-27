package com.payments.api.domain.exception;

public class InvalidClientException extends RuntimeException {

    public InvalidClientException(String message) {
        super(message);
    }
}
