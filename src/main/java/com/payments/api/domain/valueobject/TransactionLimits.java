package com.payments.api.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

public class TransactionLimits {
    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;
    private final long maxDailyTransactions;

    public TransactionLimits(BigDecimal minAmount, BigDecimal maxAmount, long maxDailyTransactions) {
        if (minAmount == null || minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum amount must be non-negative");
        }
        if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Maximum amount must be positive");
        }
        if (minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Minimum amount cannot be greater than maximum amount");
        }
        if (maxDailyTransactions < 0) {
            throw new IllegalArgumentException("Max daily transactions must be non-negative");
        }
        
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.maxDailyTransactions = maxDailyTransactions;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public long getMaxDailyTransactions() {
        return maxDailyTransactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionLimits that = (TransactionLimits) o;
        return maxDailyTransactions == that.maxDailyTransactions &&
                Objects.equals(minAmount, that.minAmount) &&
                Objects.equals(maxAmount, that.maxAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minAmount, maxAmount, maxDailyTransactions);
    }
}
