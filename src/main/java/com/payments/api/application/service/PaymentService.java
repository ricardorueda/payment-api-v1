package com.payments.api.application.service;

import com.payments.api.application.port.in.ProcessPaymentUseCase;
import com.payments.api.application.port.out.PaymentRepositoryPort;
import com.payments.api.domain.model.Payment;
import com.payments.api.domain.valueobject.Money;
import com.payments.api.domain.valueobject.PaymentMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService implements ProcessPaymentUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;

    public PaymentService(PaymentRepositoryPort paymentRepositoryPort) {
        this.paymentRepositoryPort = paymentRepositoryPort;
    }

    @Override
    public Payment processPayment(BigDecimal amount, PaymentMethod paymentMethod) {
        Money money = new Money(amount);
        Payment payment = new Payment(money, paymentMethod);
        return paymentRepositoryPort.save(payment);
    }

    @Override
    public Optional<Payment> findPaymentById(Long id) {
        return paymentRepositoryPort.findById(id);
    }

    @Override
    public List<Payment> findAllPayments() {
        return paymentRepositoryPort.findAll();
    }
}

