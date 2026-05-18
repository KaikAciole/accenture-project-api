# Assistant

## Responsabilidade
Assistente virtual com IA da plataforma. Recebe perguntas do usuário e devolve respostas em streaming (Server-Sent Events) usando o modelo **Google Gemini**. Inclui:
- Rate limiting por usuário e por IP (Bucket4j).
- Controle de concorrência para evitar requisições paralelas do mesmo usuário.
- Não possui banco de dados — é stateless.

## Stack
- Java 21
- Spring Boot 3.5.14 *(versão diferente dos demais serviços, que estão em 4.0.6)*
- Spring AI (Google Gemini)
- Bucket4j (rate limiting)
- SSE (Server-Sent Events) para streaming da resposta

## Pré-requisitos
- JDK 21 instalado
- Maven Wrapper (já incluso)
- Chave de API do Google Gemini

## Variáveis de ambiente
| Variável | Default | Descrição |
|---|---|---|
| `GEMINI_API_KEY` | *(obrigatório)* | Chave da API do Google Gemini. Sem ela o serviço não sobe corretamente. |

Limites configurados em `application.yml`:
- Por usuário: 10 req/min, 100 req/dia
- Por IP: 30 req/min, 300 req/dia
- Modelo: `gemini-2.5-flash`

## Endpoints principais
| Método | Caminho | Descrição |
|---|---|---|
| POST | `/assistant/ask` | Envia uma pergunta ao assistente e recebe a resposta em streaming SSE. |

## Como rodar
```bash
# Linux/Mac
export GEMINI_API_KEY=sua-chave-aqui
./mvnw spring-boot:run
```

```powershell
# Windows PowerShell
$env:GEMINI_API_KEY = "sua-chave-aqui"
.\mvnw.cmd spring-boot:run
```

Ou empacotando antes:
```bash
./mvnw clean package
java -jar target/assistant-0.0.1-SNAPSHOT.jar
```

O serviço sobe em `http://localhost:8087`.

## Testes
```bash
./mvnw test
```

Inclui testes do controller, do gateway que conversa com o Gemini e dos filtros de rate limit / concorrência.
