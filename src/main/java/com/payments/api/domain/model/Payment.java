package com.payments.api.domain.model;

import com.payments.api.domain.valueobject.Money;
import com.payments.api.domain.valueobject.PaymentMethod;
import com.payments.api.domain.valueobject.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public class Payment {
    private Long id;
    private Money money;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    public Payment(Long id, Money money, PaymentMethod paymentMethod, PaymentStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.money = money;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Payment(Money money, PaymentMethod paymentMethod) {
        this.money = money;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void approve() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be approved");
        }
        this.status = PaymentStatus.APPROVED;
    }

    public void reject() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be rejected");
        }
        this.status = PaymentStatus.REJECTED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Money getMoney() {
        return money;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

