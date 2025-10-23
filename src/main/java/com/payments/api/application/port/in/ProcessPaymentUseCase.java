package com.payments.api.application.port.in;

import com.payments.api.domain.model.Payment;
import com.payments.api.domain.valueobject.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProcessPaymentUseCase {
    
    Payment processPayment(BigDecimal amount, PaymentMethod paymentMethod);
    
    Optional<Payment> findPaymentById(Long id);
    
    List<Payment> findAllPayments();
}

