package com.payments.api.adapter.out.persistence;

import com.payments.api.adapter.out.persistence.entity.PaymentEntity;
import com.payments.api.domain.valueobject.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    long countByCreatedAtBetweenAndPaymentMethod(LocalDateTime start, LocalDateTime end, PaymentMethod paymentMethod);
}

