package com.payments.api.application.port.out;

import com.payments.api.domain.model.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepositoryPort {
    
    Payment save(Payment payment);
    
    Optional<Payment> findById(Long id);
    
    List<Payment> findAll();
}

