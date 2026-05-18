# Auth

## Responsabilidade
Serviço responsável por toda a autenticação e identidade básica do usuário:
- Registro de credenciais (e-mail + senha).
- Login com geração de token JWT.
- Fluxo de "esqueci minha senha" (gera token de reset com TTL configurável).
- Reset efetivo de senha a partir do token.

Após registro publica o evento `user.registered` no RabbitMQ, consumido pelos serviços `customer`, `payment` e `notification`.

## Stack
- Java 21
- Spring Boot 4.0.6
- Spring Security + JWT (HS256)
- Spring Data JPA + H2 em memória
- Spring AMQP (RabbitMQ)

## Pré-requisitos
- JDK 21 instalado
- Maven Wrapper (já incluso)
- RabbitMQ rodando em `localhost:5672` (usuário/senha `guest/guest`)

## Variáveis de ambiente
| Variável | Default | Descrição |
|---|---|---|
| `RABBITMQ_URL` | `amqp://guest:guest@localhost:5672` | URL de conexão do RabbitMQ. |
| `JWT_SECRET` | `my-super-secret-key-change-me-in-production` | Segredo HS256 para assinar e validar tokens. Deve ser o mesmo no `api-gateway`. |
| `PASSWORD_RESET_TTL_MINUTES` | `30` | Tempo de vida (em minutos) do token de reset de senha. |

## Endpoints principais
| Método | Caminho | Descrição |
|---|---|---|
| POST | `/api/v1/auth/register` | Registra novas credenciais. Publica `user.registered`. |
| POST | `/api/v1/auth/login` | Autentica e retorna JWT. |
| POST | `/api/v1/auth/forgot-password` | Inicia o fluxo de reset de senha (envia evento ao `notification`). |
| POST | `/api/v1/auth/reset-password` | Confirma novo password a partir do token de reset. |

Console H2: `http://localhost:8081/h2-console` (JDBC URL `jdbc:h2:mem:authdb`).

## Como rodar
```bash
./mvnw spring-boot:run
```

Ou empacotando antes:
```bash
./mvnw clean package
java -jar target/auth-0.0.1-SNAPSHOT.jar
```

No Windows (PowerShell), troque `./mvnw` por `.\mvnw.cmd`.

O serviço sobe em `http://localhost:8081`.

## Testes
```bash
./mvnw test
```
