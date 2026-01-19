package com.payments.api.domain.exception;

public class DailyLimitExceededException extends PaymentException {
    
    public DailyLimitExceededException(String message) {
        super(message);
    }

    public DailyLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
