# Payment

## Responsabilidade
Processa pagamentos e mantém a carteira (wallet) de cada cliente:
- Recebe eventos de pedido criado e dispara a cobrança correspondente.
- Permite gerenciar saldo do cliente (top-up) e registrar transações.
- Possui integração com o gateway externo **AbacatePay** (criação de cobranças e webhook).
- Publica eventos de pagamento (`payment.events`) para o `order`, o `inventory` e o `notification`.
- Reage ao evento `user.registered` para criar uma wallet inicial para o cliente.

## Stack
- Java 21
- Spring Boot 4.0.6
- Spring Data JPA + H2 em memória
- Spring AMQP (RabbitMQ) — exchanges: `payment.events`, `order.exchange`, `auth.exchange`
- Spring Retry (resiliência em chamadas externas)
- AbacatePay (HTTP)

## Pré-requisitos
- JDK 21 instalado
- Maven Wrapper (já incluso)
- RabbitMQ rodando em `localhost:5672` (usuário/senha `guest/guest`)
- Chave de API do AbacatePay (sandbox ou produção)

## Variáveis de ambiente
| Variável | Default | Descrição |
|---|---|---|
| `RABBITMQ_URL` | `amqp://guest:guest@localhost:5672` | URL de conexão do RabbitMQ. |
| `ABACATEPAY_API_KEY` | *(obrigatório)* | Chave de API do AbacatePay. |
| `ABACATEPAY_WEBHOOK_SECRET` | *(obrigatório)* | Segredo de validação do webhook do AbacatePay. |
| `PAYMENT_COMPANY_WALLET_OWNER_ID` | `00000000-0000-0000-0000-000000000001` | Id da carteira "da empresa", usada como contraparte das transações. |

## Endpoints principais
| Método | Caminho | Descrição |
|---|---|---|
| POST | `/payments` | Cria um pagamento. |
| GET | `/payments` | Lista pagamentos. |
| PATCH | `/payments/{id}/status` | Atualiza o status de um pagamento. |
| GET | `/wallets/{customerId}` | Consulta a carteira do cliente. |
| POST | `/wallets/{customerId}/top-up` | Adiciona saldo à carteira. |

Console H2: `http://localhost:8086/h2-console` (JDBC URL `jdbc:h2:mem:payment_db`).

## Como rodar
```bash
./mvnw spring-boot:run
```

Ou empacotando antes:
```bash
./mvnw clean package
java -jar target/payment-0.0.1-SNAPSHOT.jar
```

No Windows (PowerShell), troque `./mvnw` por `.\mvnw.cmd`.

O serviço sobe em `http://localhost:8086`.

## Testes
```bash
./mvnw test
```

Inclui testes E2E (`PaymentLifecycleE2eTest`, `WalletLifecycleE2eTest`), testes de listener e de mapper.
