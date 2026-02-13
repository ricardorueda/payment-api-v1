package com.payments.api.application.port.out;

import com.payments.api.domain.model.Payment;
import com.payments.api.domain.valueobject.PaymentMethod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepositoryPort {
    
    Payment save(Payment payment);
    
    Optional<Payment> findById(Long id);
    
    List<Payment> findAll();
    
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    long countByCreatedAtBetweenAndPaymentMethod(LocalDateTime start, LocalDateTime end, PaymentMethod paymentMethod);
}

