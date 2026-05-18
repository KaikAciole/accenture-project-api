# Inventory

## Responsabilidade
Cuida do catálogo de produtos e do controle de estoque:
- CRUD de produtos.
- Reservas de estoque (`StockReservation`) com ciclo de status (criada / confirmada / cancelada).
- Reage a eventos do `order` (confirmar/cancelar reserva) e do `payment` (efetivar baixa ou devolver itens ao estoque quando o pagamento é cancelado).

## Stack
- Java 21
- Spring Boot 4.0.6
- Spring Data JPA + H2 em memória
- Spring AMQP (RabbitMQ) — múltiplas exchanges: `order.exchange`, `payment.events`, `stock.exchange`
- Lombok

## Pré-requisitos
- JDK 21 instalado
- Maven Wrapper (já incluso)
- RabbitMQ rodando em `localhost:5672` (usuário/senha `guest/guest`)

## Variáveis de ambiente
| Variável | Default | Descrição |
|---|---|---|
| `RABBITMQ_URL` | `amqp://guest:guest@localhost:5672` | URL de conexão do RabbitMQ. |

## Endpoints principais
| Método | Caminho | Descrição |
|---|---|---|
| GET | `/products` | Lista produtos. |
| POST | `/products` | Cria produto. |
| PATCH | `/products/{id}` | Atualiza produto. |
| POST | `/stock-reservations` | Cria reserva de estoque. |
| GET | `/stock-reservations` | Lista reservas paginadas (filtro opcional por status). |
| PATCH | `/stock-reservations/{id}/status` | Altera o status de uma reserva. |

Console H2: `http://localhost:8083/h2-console` (JDBC URL `jdbc:h2:mem:inventory_db`).

## Como rodar
```bash
./mvnw spring-boot:run
```

Ou empacotando antes:
```bash
./mvnw clean package
java -jar target/inventory-0.0.1-SNAPSHOT.jar
```

No Windows (PowerShell), troque `./mvnw` por `.\mvnw.cmd`.

O serviço sobe em `http://localhost:8083`.

## Testes
```bash
./mvnw test
```
