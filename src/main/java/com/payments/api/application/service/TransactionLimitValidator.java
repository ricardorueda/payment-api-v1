package com.payments.api.application.service;

import com.payments.api.application.port.in.ValidateTransactionLimitUseCase;
import com.payments.api.application.port.out.PaymentRepositoryPort;
import com.payments.api.config.TransactionLimitConfig;
import com.payments.api.domain.exception.DailyLimitExceededException;
import com.payments.api.domain.exception.MaximumAmountException;
import com.payments.api.domain.exception.MinimumAmountException;
import com.payments.api.domain.valueobject.PaymentMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class TransactionLimitValidator implements ValidateTransactionLimitUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final TransactionLimitConfig limitConfig;

    public TransactionLimitValidator(
            PaymentRepositoryPort paymentRepositoryPort,
            TransactionLimitConfig limitConfig) {
        this.paymentRepositoryPort = paymentRepositoryPort;
        this.limitConfig = limitConfig;
    }

    @Override
    public void validate(BigDecimal amount, PaymentMethod paymentMethod) {
        validateAmountLimits(amount, paymentMethod);
        validateDailyLimit();
    }

    private void validateAmountLimits(BigDecimal amount, PaymentMethod paymentMethod) {
        BigDecimal globalMin = limitConfig.getGlobal().getMin();
        BigDecimal globalMax = limitConfig.getGlobal().getMax();
        
        String methodKey = paymentMethod.name().toLowerCase();
        TransactionLimitConfig.MethodLimits methodLimits = limitConfig.getMethodLimits(methodKey);
        
        BigDecimal effectiveMin = globalMin;
        BigDecimal effectiveMax = globalMax;
        
        if (methodLimits != null) {
            if (methodLimits.getMin() != null) {
                effectiveMin = methodLimits.getMin().compareTo(globalMin) > 0 
                    ? methodLimits.getMin() 
                    : globalMin;
            }
            if (methodLimits.getMax() != null) {
                effectiveMax = methodLimits.getMax().compareTo(globalMax) < 0 
                    ? methodLimits.getMax() 
                    : globalMax;
            }
        }
        
        if (amount.compareTo(effectiveMin) < 0) {
            throw new MinimumAmountException(
                String.format("Valor mínimo permitido é %s. Valor informado: %s", effectiveMin, amount)
            );
        }
        
        if (amount.compareTo(effectiveMax) > 0) {
            throw new MaximumAmountException(
                String.format("Valor máximo permitido é %s. Valor informado: %s", effectiveMax, amount)
            );
        }
    }

    private void validateDailyLimit() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        long currentDailyCount = paymentRepositoryPort.countByCreatedAtBetween(startOfDay, endOfDay);
        long maxDailyTransactions = limitConfig.getDaily().getMaxTransactions();
        
        if (currentDailyCount >= maxDailyTransactions) {
            throw new DailyLimitExceededException(
                String.format("Limite diário de transações excedido. Limite: %d, Atual: %d", 
                    maxDailyTransactions, currentDailyCount)
            );
        }
    }
}
