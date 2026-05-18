# Order

## Responsabilidade
Centraliza a criação e o ciclo de vida dos pedidos:
- Cria pedido a partir do cliente autenticado e dos itens informados.
- Consulta o `customer` (Feign) para validar o cliente e obter endereço de entrega.
- Consulta o `inventory` (Feign) para validar/criar a reserva de estoque.
- Publica eventos no RabbitMQ (`order.exchange`) que disparam o pagamento e a notificação.
- Consome eventos do `inventory` e do `payment` para atualizar o status do pedido (confirmado / cancelado).

## Stack
- Java 21
- Spring Boot 4.0.6
- Spring Data JPA + H2 em memória
- Spring Cloud OpenFeign (chamadas síncronas a `customer` e `inventory`)
- Spring AMQP (RabbitMQ) com retry exponencial (5 tentativas)
- Lombok

## Pré-requisitos
- JDK 21 instalado
- Maven Wrapper (já incluso)
- RabbitMQ rodando em `localhost:5672` (usuário/senha `guest/guest`)
- Serviços `customer` (`:8082`) e `inventory` (`:8083`) no ar quando for criar pedido

## Variáveis de ambiente
| Variável | Default | Descrição |
|---|---|---|
| `RABBITMQ_URL` | `amqp://guest:guest@localhost:5672` | URL de conexão do RabbitMQ. |
| `CUSTOMER_SERVICE_URL` | `http://localhost:8082` | URL base do `customer` (Feign). |
| `INVENTORY_SERVICE_URL` | `http://localhost:8083` | URL base do `inventory` (Feign). |
| `INTERNAL_API_SECRET` | `senha-secreta-microsservicos-1234` | Segredo compartilhado para chamadas internas. Deve bater com os demais serviços. |

## Endpoints principais
| Método | Caminho | Descrição |
|---|---|---|
| POST | `/orders` | Cria um novo pedido. |
| GET | `/orders` | Lista pedidos paginados. |
| GET | `/orders/{id}` | Busca um pedido por id. |
| GET | `/api/v1/orders/my-orders` | Lista os pedidos do cliente autenticado (token JWT). |

Console H2: `http://localhost:8085/h2-console` (JDBC URL `jdbc:h2:mem:order_db`).

## Como rodar
```bash
./mvnw spring-boot:run
```

Ou empacotando antes:
```bash
./mvnw clean package
java -jar target/order-0.0.1-SNAPSHOT.jar
```

No Windows (PowerShell), troque `./mvnw` por `.\mvnw.cmd`.

O serviço sobe em `http://localhost:8085`.

## Testes
```bash
./mvnw test
```

Inclui testes unitários, de listeners (`OrderEventListener`, `StockEventListener`) e um teste end-to-end (`OrderEndToEndTest`).
