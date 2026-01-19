package com.payments.api.adapter.in.rest;

import com.payments.api.adapter.in.rest.dto.PaymentRequest;
import com.payments.api.adapter.in.rest.dto.PaymentResponse;
import com.payments.api.application.port.in.ProcessPaymentUseCase;
import com.payments.api.domain.model.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Endpoints para gerenciamento de pagamentos")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
    }

    @PostMapping
    @Operation(summary = "Processa um novo pagamento", description = "Cria e processa um pagamento com base nos dados fornecidos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pagamento criado com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos", content = @Content),
        @ApiResponse(responseCode = "422", description = "Limite de valor mínimo ou máximo excedido", content = @Content),
        @ApiResponse(responseCode = "429", description = "Limite diário de transações excedido", content = @Content),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content)
    })
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = processPaymentUseCase.processPayment(
                request.getAmount(),
                request.getPaymentMethod()
        );
        PaymentResponse response = toResponse(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca pagamento por ID", description = "Retorna os detalhes de um pagamento específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pagamento encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado", content = @Content)
    })
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return processPaymentUseCase.findPaymentById(id)
                .map(payment -> ResponseEntity.ok(toResponse(payment)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Lista todos os pagamentos", description = "Retorna uma lista com todos os pagamentos cadastrados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class)))
    })
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        List<PaymentResponse> payments = processPaymentUseCase.findAllPayments().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payments);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMoney().getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}

