package com.payments.api.application.port.in;

import com.payments.api.domain.valueobject.PaymentMethod;

import java.math.BigDecimal;

public interface ValidateTransactionLimitUseCase {
    
    void validate(BigDecimal amount, PaymentMethod paymentMethod);
}
