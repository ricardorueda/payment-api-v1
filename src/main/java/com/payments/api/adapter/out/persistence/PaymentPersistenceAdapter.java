package com.payments.api.adapter.out.persistence;

import com.payments.api.adapter.out.persistence.entity.PaymentEntity;
import com.payments.api.application.port.out.PaymentRepositoryPort;
import com.payments.api.domain.model.Payment;
import com.payments.api.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PaymentPersistenceAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository paymentJpaRepository;

    public PaymentPersistenceAdapter(PaymentJpaRepository paymentJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        PaymentEntity savedEntity = paymentJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Payment> findAll() {
        return paymentJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private PaymentEntity toEntity(Payment payment) {
        return new PaymentEntity(
                payment.getId(),
                payment.getMoney().getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }

    private Payment toDomain(PaymentEntity entity) {
        Money money = new Money(entity.getAmount());
        return new Payment(
                entity.getId(),
                money,
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}

