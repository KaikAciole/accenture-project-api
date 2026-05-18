# Notification

## Responsabilidade
Serviço de comunicação com o usuário final. Consome eventos publicados pelos demais microserviços e envia e-mails:
- `user.registered` (boas-vindas).
- Reset de senha (link para o frontend).
- Pedido criado, confirmado, cancelado.
- Pagamento aprovado, recusado, cancelado.

Persiste o histórico de cada notificação enviada e expõe um endpoint de consulta.

## Stack
- Java 21
- Spring Boot 4.0.6
- Spring Data JPA + H2 em memória
- Spring AMQP (RabbitMQ) — 8+ listeners
- Spring Mail (SMTP)
- RestClient (consulta dados do cliente no `customer`)

## Pré-requisitos
- JDK 21 instalado
- Maven Wrapper (já incluso)
- RabbitMQ rodando em `localhost:5672` (usuário/senha `guest/guest`)
- Conta Gmail com **senha de app** habilitada (para SMTP)
- Serviço `customer` rodando em `:8082`

## Variáveis de ambiente
| Variável | Default | Descrição |
|---|---|---|
| `RABBITMQ_URL` | `amqp://guest:guest@localhost:5672` | URL de conexão do RabbitMQ. |
| `SMTP_MAIL_USERNAME` | *(obrigatório)* | E-mail da conta Gmail usada para envio. |
| `SMTP_MAIL_PASSWORD` | *(obrigatório)* | Senha de app do Gmail. |
| `CUSTOMER_SERVICE_URL` | `http://localhost:8082` | URL base do `customer`. |
| `FRONTEND_PASSWORD_RESET_URL` | `http://localhost:5173/reset-password` | URL do frontend incluída no link de reset de senha. |
| `INTERNAL_API_SECRET` | `senha-secreta-microsservicos-1234` | Segredo compartilhado para chamadas internas ao `customer`. |

## Endpoints principais
| Método | Caminho | Descrição |
|---|---|---|
| GET | `/notifications/{id}` | Consulta uma notificação enviada. |

Console H2: `http://localhost:8084/h2-console` (JDBC URL `jdbc:h2:mem:notifications_db`).

## Como rodar
```bash
./mvnw spring-boot:run
```

Ou empacotando antes:
```bash
./mvnw clean package
java -jar target/notification-0.0.1-SNAPSHOT.jar
```

No Windows (PowerShell), troque `./mvnw` por `.\mvnw.cmd`.

O serviço sobe em `http://localhost:8084`.

## Testes
```bash
./mvnw test
```

Inclui testes unitários, de listeners, integração com repositório JPA e do controller.
