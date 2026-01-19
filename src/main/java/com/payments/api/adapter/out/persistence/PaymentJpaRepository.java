package com.payments.api.adapter.out.persistence;

import com.payments.api.adapter.out.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

