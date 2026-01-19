package com.payments.api.domain.exception;

public class MaximumAmountException extends PaymentException {
    
    public MaximumAmountException(String message) {
        super(message);
    }

    public MaximumAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}
