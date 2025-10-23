package com.payments.api.adapter.in.rest.dto;

import com.payments.api.domain.valueobject.PaymentMethod;
import com.payments.api.domain.valueobject.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Resposta com os dados do pagamento processado")
public class PaymentResponse {

    @Schema(description = "Identificador único do pagamento", example = "1")
    private Long id;

    @Schema(description = "Valor do pagamento", example = "100.50")
    private BigDecimal amount;

    @Schema(description = "Método de pagamento utilizado", example = "PIX")
    private PaymentMethod paymentMethod;

    @Schema(description = "Status do pagamento", example = "PENDING")
    private PaymentStatus status;

    @Schema(description = "Data e hora de criação do pagamento", example = "2025-10-23T10:30:00")
    private LocalDateTime createdAt;

    public PaymentResponse() {
    }

    public PaymentResponse(Long id, BigDecimal amount, PaymentMethod paymentMethod, PaymentStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

