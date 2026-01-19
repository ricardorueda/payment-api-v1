package com.payments.api.domain.exception;

public class TransactionLimitExceededException extends PaymentException {
    
    public TransactionLimitExceededException(String message) {
        super(message);
    }

    public TransactionLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
