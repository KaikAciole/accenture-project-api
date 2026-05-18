# API Gateway

## Responsabilidade
Ponto de entrada único da plataforma. É responsável por:
- Rotear requisições externas para os microserviços internos (auth, customer, inventory, notification, order, payment, assistant).
- Validar o token JWT antes de encaminhar chamadas autenticadas.
- Orquestrar o fluxo de registro de usuário (`register-flow`), coordenando criação no `auth` e no `customer` em uma única chamada externa.
- Expor proxy para consultas de CEP (ViaCEP via `customer`).

Não possui banco de dados nem persistência própria.

## Stack
- Java 21
- Spring Boot 4.0.6
- Spring WebFlux / WebClient (chamadas síncronas HTTP)
- Validação JWT (chave compartilhada com o `auth`)
- SpringDoc OpenAPI

## Pré-requisitos
- JDK 21 instalado
- Maven Wrapper (já incluso no projeto: `mvnw` / `mvnw.cmd`)
- Os serviços downstream rodando localmente nas portas padrão:
  - `auth` em `:8081`
  - `customer` em `:8082`
  - `inventory` em `:8083`
  - `notification` em `:8084`
  - `order` em `:8085`
  - `payment` em `:8086`
  - `assistant` em `:8087`

## Variáveis de ambiente
| Variável | Default | Descrição |
|---|---|---|
| `JWT_SECRET` | `my-super-secret-key-change-me-in-production` | Segredo HS256 usado para validar tokens emitidos pelo `auth`. Precisa ser o mesmo nos dois serviços. |

## Endpoints principais
| Método | Caminho | Descrição |
|---|---|---|
| POST | `/api/v1/gateway/register-flow` | Orquestra o registro de um novo usuário (cria credencial no `auth` e cliente no `customer`). |
| POST | `/api/v1/auth/**` | Proxy para o serviço `auth` (login, registro, reset de senha). |
| GET | `/api/v1/cep/lookup` | Proxy de consulta de CEP via `customer`. |
| POST | `/orders/my-orders` | Proxy autenticado para pedidos do cliente logado. |

Documentação Swagger UI: `http://localhost:8080/swagger-ui.html` (após subir).

## Como rodar
```bash
./mvnw spring-boot:run
```

Ou empacotando antes:
```bash
./mvnw clean package
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

No Windows (PowerShell), troque `./mvnw` por `.\mvnw.cmd`.

O serviço sobe em `http://localhost:8080`.

## Testes
```bash
./mvnw test
```
