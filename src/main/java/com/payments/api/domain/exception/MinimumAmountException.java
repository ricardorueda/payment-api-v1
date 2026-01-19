package com.payments.api.domain.exception;

public class MinimumAmountException extends PaymentException {
    
    public MinimumAmountException(String message) {
        super(message);
    }

    public MinimumAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}
