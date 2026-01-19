package com.payments.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "payment.limits")
public class TransactionLimitConfig {

    private GlobalLimits global = new GlobalLimits();
    private Map<String, MethodLimits> methods = new HashMap<>();
    private DailyLimits daily = new DailyLimits();

    @PostConstruct
    public void init() {
        if (methods == null) {
            methods = new HashMap<>();
        }
    }

    public GlobalLimits getGlobal() {
        return global;
    }

    public void setGlobal(GlobalLimits global) {
        this.global = global;
    }

    public Map<String, MethodLimits> getMethods() {
        return methods;
    }

    public void setMethods(Map<String, MethodLimits> methods) {
        this.methods = methods;
    }

    public DailyLimits getDaily() {
        return daily;
    }

    public void setDaily(DailyLimits daily) {
        this.daily = daily;
    }

    public static class GlobalLimits {
        private BigDecimal min = BigDecimal.valueOf(10.00);
        private BigDecimal max = BigDecimal.valueOf(100000.00);

        public BigDecimal getMin() {
            return min;
        }

        public void setMin(BigDecimal min) {
            this.min = min;
        }

        public BigDecimal getMax() {
            return max;
        }

        public void setMax(BigDecimal max) {
            this.max = max;
        }
    }

    public static class MethodLimits {
        private BigDecimal min;
        private BigDecimal max;

        public BigDecimal getMin() {
            return min;
        }

        public void setMin(BigDecimal min) {
            this.min = min;
        }

        public BigDecimal getMax() {
            return max;
        }

        public void setMax(BigDecimal max) {
            this.max = max;
        }
    }

    public static class DailyLimits {
        private long maxTransactions = 1000L;

        public long getMaxTransactions() {
            return maxTransactions;
        }

        public void setMaxTransactions(long maxTransactions) {
            this.maxTransactions = maxTransactions;
        }
    }

    public MethodLimits getMethodLimits(String methodKey) {
        return methods.get(methodKey);
    }
}
