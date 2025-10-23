# Payments API - Arquitetura Hexagonal

API REST de pagamentos desenvolvida com Java 17, Spring Boot 3.x e Maven, implementando Arquitetura Hexagonal (Ports & Adapters).

## Tecnologias

- **Java 17** (LTS)
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **H2 Database** (banco em memória)
- **Maven**

## Arquitetura

O projeto segue a **Arquitetura Hexagonal (Ports & Adapters)** com clara separação de responsabilidades:

### Camadas

#### 1. Domain (Núcleo)
- **Model**: `Payment` - Entidade de domínio pura (POJO)
- **Value Objects**: `Money`, `PaymentStatus`, `PaymentMethod`
- **Exceptions**: `PaymentException`
- **Regra**: Sem dependências externas, sem anotações de frameworks

#### 2. Application (Casos de Uso)
- **Ports In**: `ProcessPaymentUseCase` - Interface de entrada
- **Ports Out**: `PaymentRepositoryPort` - Interface de saída
- **Service**: `PaymentService` - Implementação do caso de uso

#### 3. Adapters
- **In (REST)**: `PaymentController`, DTOs
- **Out (Persistence)**: `PaymentPersistenceAdapter`, `PaymentEntity`, `PaymentJpaRepository`

## Estrutura do Projeto

```
src/main/java/com/payments/api/
├── PaymentsApiApplication.java
├── domain/
│   ├── model/
│   │   └── Payment.java
│   ├── valueobject/
│   │   ├── Money.java
│   │   ├── PaymentMethod.java
│   │   └── PaymentStatus.java
│   └── exception/
│       └── PaymentException.java
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   └── ProcessPaymentUseCase.java
│   │   └── out/
│   │       └── PaymentRepositoryPort.java
│   └── service/
│       └── PaymentService.java
├── adapter/
│   ├── in/
│   │   └── rest/
│   │       ├── PaymentController.java
│   │       └── dto/
│   │           ├── PaymentRequest.java
│   │           └── PaymentResponse.java
│   └── out/
│       └── persistence/
│           ├── PaymentJpaRepository.java
│           ├── PaymentPersistenceAdapter.java
│           └── entity/
│               └── PaymentEntity.java
└── config/
    └── BeanConfiguration.java
```

## Documentação da API (Swagger)

A API possui documentação interativa usando **Swagger/OpenAPI 3.0**.

**Acessar a documentação:**
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`

A interface do Swagger permite testar todos os endpoints diretamente do navegador.

## Endpoints da API

### Criar Pagamento
```http
POST /api/v1/payments
Content-Type: application/json

{
  "amount": 100.50,
  "paymentMethod": "PIX"
}
```

### Buscar Pagamento por ID
```http
GET /api/v1/payments/{id}
```

### Listar Todos os Pagamentos
```http
GET /api/v1/payments
```

## Métodos de Pagamento

- `CREDIT_CARD` - Cartão de Crédito
- `DEBIT_CARD` - Cartão de Débito
- `PIX` - PIX
- `BOLETO` - Boleto Bancário

## Status do Pagamento

- `PENDING` - Pendente
- `APPROVED` - Aprovado
- `REJECTED` - Rejeitado

## Como Executar

### Pré-requisitos
- Java 17
- Maven 3.6+

### Executar a Aplicação

**Opção 1: Usando Maven**
```bash
mvn spring-boot:run
```

**Opção 2: Usando o script auxiliar**
```bash
./build-and-run.sh
```

A aplicação estará disponível em: `http://localhost:8080`

**URLs importantes:**
- API: `http://localhost:8080/api/v1/payments`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`

### Console H2

Acessar o console do banco H2 em: `http://localhost:8080/h2-console`

**Configurações de conexão:**
- JDBC URL: `jdbc:h2:mem:paymentsdb`
- Username: `sa`
- Password: _(deixar vazio)_

## Princípios Aplicados

- **Hexagonal Architecture**: Isolamento do domínio, dependências apontam para dentro
- **Clean Code**: Nomes descritivos, métodos coesos e focados
- **SOLID**:
  - SRP: Cada classe tem uma única responsabilidade
  - OCP: Extensível através de ports
  - DIP: Domínio não depende de infraestrutura
- **DRY**: Reutilização via Value Objects
- **KISS**: Estrutura simples e clara

## Validações

- Valor do pagamento deve ser positivo
- Método de pagamento obrigatório
- Apenas pagamentos com status PENDING podem ser aprovados ou rejeitados

