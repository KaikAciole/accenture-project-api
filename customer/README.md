# Customer

## Responsabilidade
Gerencia os dados pessoais e endereços dos clientes da plataforma:
- CRUD de clientes (nome, documento, contato, etc.).
- CRUD de endereços vinculados ao cliente.
- Lookup de CEP via integração com a API pública ViaCEP.
- Endpoint interno (`/internal/customers`) chamado pelo `auth` após registro, protegido por segredo compartilhado.

## Stack
- Java 21
- Spring Boot 4.0.6
- Spring Data JPA + H2 em memória
- WebClient (chamada ao ViaCEP)
- Lombok

## Pré-requisitos
- JDK 21 instalado
- Maven Wrapper (já incluso)
- Acesso à internet (para chamar `https://viacep.com.br/ws`)
- Serviço `auth` rodando em `:8081` quando o fluxo de registro for exercitado

## Variáveis de ambiente
| Variável | Default | Descrição |
|---|---|---|
| `AUTH_SERVICE_URL` | `http://localhost:8081` | URL base do serviço `auth`. |
| `INTERNAL_API_SECRET` | `senha-secreta-microsservicos-1234` | Segredo compartilhado para chamadas internas (header). Deve ser o mesmo nos demais serviços que conversam com `customer`. |

## Endpoints principais
| Método | Caminho | Descrição |
|---|---|---|
| POST | `/internal/customers` | Criação interna do cliente (chamada pelo `auth` após registro). |
| GET | `/customers` | Lista clientes com paginação. |
| GET | `/customers/{id}` | Busca um cliente por id. |
| PATCH | `/customers/{id}` | Atualiza parcialmente um cliente. |
| DELETE | `/customers/{id}` | Remove um cliente. |
| POST | `/addresses/{customerId}` | Cria endereço vinculado a um cliente. |
| GET | `/cep/lookup` | Consulta CEP via ViaCEP. |

Console H2: `http://localhost:8082/h2-console` (JDBC URL `jdbc:h2:mem:customer_db`).

## Como rodar
```bash
./mvnw spring-boot:run
```

Ou empacotando antes:
```bash
./mvnw clean package
java -jar target/customer-0.0.1-SNAPSHOT.jar
```

No Windows (PowerShell), troque `./mvnw` por `.\mvnw.cmd`.

O serviço sobe em `http://localhost:8082`.

## Testes
```bash
./mvnw test
```

Inclui testes unitários, de mapeamento/paginação e um teste E2E de ciclo de vida (`CustomerLifecycleE2eTest`).
