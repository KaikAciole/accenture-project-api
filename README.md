# Accenture Project API

Plataforma de e-commerce educacional construída como projeto final do treinamento da Accenture. É um monorepo com **8 microserviços Spring Boot** que, juntos, cobrem o fluxo completo de uma loja: autenticação, cadastro de cliente, catálogo/estoque, criação de pedido, pagamento via PIX, notificações por e-mail e um assistente virtual com IA.

A comunicação entre os serviços é **síncrona** (Feign/WebClient — usada quando uma resposta imediata é necessária, ex.: `order` consultando `customer` e `inventory`) e **assíncrona** (RabbitMQ — usada para propagar eventos como "pedido criado", "pagamento aprovado", "estoque reservado").

## Serviços

| Serviço | Porta | Descrição |
|---|---|---|
| [api-gateway](./api-gateway/README.md) | 8080 | Ponto de entrada externo, valida JWT e roteia para os demais serviços. |
| [auth](./auth/README.md) | 8081 | Autenticação, registro, JWT e reset de senha. |
| [customer](./customer/README.md) | 8082 | Cadastro de clientes, endereços e lookup de CEP (ViaCEP). |
| [inventory](./inventory/README.md) | 8083 | Catálogo de produtos e reservas de estoque. |
| [notification](./notification/README.md) | 8084 | Envio de e-mails disparados por eventos. |
| [order](./order/README.md) | 8085 | Criação e ciclo de vida dos pedidos. |
| [payment](./payment/README.md) | 8086 | Pagamentos via PIX (AbacatePay) e carteira (wallet). |
| [assistant](./assistant/README.md) | 8087 | Assistente virtual com IA (Google Gemini, streaming SSE). |

## Pré-requisitos
- **JDK 21**
- **Git**
- **RabbitMQ** rodando localmente (ver passo a passo abaixo)
- **ngrok** instalado (para expor o webhook do AbacatePay)
- Conta **Google** dedicada ao projeto (para o SMTP do `notification`)
- Conta **AbacatePay** homologada (para o PIX do `payment`)
- Chave de API do **Google Gemini** (para o `assistant`)

Cada serviço já traz o Maven Wrapper (`mvnw` / `mvnw.cmd`), então não é preciso instalar Maven globalmente.

---

## Configuração da infraestrutura local

### 1. RabbitMQ

Durante o treinamento **não tivemos permissão para usar Docker**, então o RabbitMQ foi instalado nativamente em cada máquina.

#### Windows
1. Instale o **Erlang/OTP** (dependência obrigatória do RabbitMQ):
   - Baixe em https://www.erlang.org/downloads e siga o instalador padrão.
2. Instale o **RabbitMQ Server**:
   - Baixe em https://www.rabbitmq.com/install-windows.html.
3. Habilite o painel de gerenciamento (abra o "RabbitMQ Command Prompt" como administrador):
   ```powershell
   rabbitmq-plugins enable rabbitmq_management
   ```
4. O serviço já sobe automaticamente como serviço do Windows. Para parar/iniciar manualmente:
   ```powershell
   net stop  RabbitMQ
   net start RabbitMQ
   ```

#### Linux / macOS
Siga as instruções oficiais: https://www.rabbitmq.com/download.html (no macOS, `brew install rabbitmq` resolve; em Debian/Ubuntu, há repositório APT oficial).

#### Verificando
- Conexão AMQP: `localhost:5672`
- Painel de gerenciamento: http://localhost:15672 (usuário/senha padrão: `guest` / `guest`)

---

### 2. Conta Google para SMTP (usada pelo `notification`)

O serviço `notification` envia e-mails via Gmail. Como o Google bloqueia login com a senha normal da conta em aplicações de terceiros, usamos o mecanismo oficial de **Senha de app**, que exige antes ativar a verificação em duas etapas.

Recomendamos criar uma **conta Google dedicada** para o projeto (não use sua conta pessoal).

Passo a passo:
1. Acesse https://myaccount.google.com/security com a conta de teste.
2. Ative a **Verificação em duas etapas** (obrigatório — sem isso o Google não libera senhas de app).
3. Acesse https://myaccount.google.com/apppasswords e gere uma nova **Senha de app**.
4. O Google exibe uma senha de 16 caracteres uma única vez. Essa é a senha que vai no `SMTP_MAIL_PASSWORD`.

Variáveis usadas pelo `notification`:
- `SMTP_MAIL_USERNAME` → endereço completo da conta (ex.: `projeto-treinamento@gmail.com`)
- `SMTP_MAIL_PASSWORD` → senha de app de 16 caracteres gerada acima

---

### 3. AbacatePay (pagamentos via PIX, usado pelo `payment`)

Passo a passo:
1. Crie uma conta em https://www.abacatepay.com.
2. Conclua a **homologação** no painel para habilitar o **modo produção** — só assim o PIX real funciona.
3. No painel, gere uma **API Key** → vai em `ABACATEPAY_API_KEY`.
4. Cadastre um **webhook** apontando para a URL pública do seu ngrok (próximo passo). O segredo gerado nesse passo vai em `ABACATEPAY_WEBHOOK_SECRET`.

---

### 4. ngrok (expor o webhook do AbacatePay)

O AbacatePay precisa chamar a nossa máquina de fora quando um pagamento é confirmado. Como o `payment` roda em `localhost:8086`, usamos o **ngrok** para gerar uma URL pública que faz proxy para a porta local.

Passo a passo:
1. Instale o ngrok:
   - Windows: baixe em https://ngrok.com/download e descompacte, ou instale via `winget install ngrok`.
   - Linux/macOS: siga https://ngrok.com/download (no macOS, `brew install ngrok`).
2. Crie uma conta gratuita em https://dashboard.ngrok.com e copie seu authtoken.
3. Autentique o ngrok localmente (uma única vez):
   ```bash
   ngrok config add-authtoken SEU_TOKEN_AQUI
   ```
4. Com o `payment` rodando em `:8086`, exponha a porta:
   ```bash
   ngrok http 8086
   ```
5. O ngrok exibe uma URL pública parecida com `https://abcd-1234.ngrok-free.app`. Use essa URL para montar o webhook que vai cadastrar no painel do AbacatePay:
   ```
   https://abcd-1234.ngrok-free.app/webhooks/abacatepay
   ```
   (O endpoint dentro do `payment` é `POST /webhooks/abacatepay`.)

> A URL gratuita do ngrok muda toda vez que ele reinicia. Sempre que reabrir o ngrok, atualize o webhook no painel do AbacatePay.

---

### 5. Google Gemini (usado pelo `assistant`)

O serviço `assistant` usa o modelo **`gemini-2.5-flash`** via API do Google. A chave foi gerada gratuitamente pelo **Google AI Studio**, que disponibiliza o `gemini-2.5-flash` no tier gratuito (com rate limits diários, suficiente para uso em desenvolvimento e demonstração do projeto).

Passo a passo:
1. Acesse https://aistudio.google.com e faça login com uma conta Google.
2. No menu lateral, abra **Get API key** (ou diretamente https://aistudio.google.com/apikey).
3. Clique em **Create API key** e copie a chave gerada.
4. Use esse valor em `GEMINI_API_KEY` ao subir o `assistant`.

> Os limites do tier gratuito são por minuto e por dia. Se atingir o limite durante testes, basta esperar a janela de reset ou gerar outra chave em uma conta Google diferente.

---

## Variáveis de ambiente compartilhadas

Para o fluxo end-to-end funcionar, mantenha **o mesmo valor** nestas variáveis em todos os serviços que as usam:

| Variável | Onde é usada |
|---|---|
| `JWT_SECRET` | `auth` (assina o token) e `api-gateway` (valida) |
| `INTERNAL_API_SECRET` | `auth`, `customer`, `order`, `notification` (header de chamadas internas) |
| `RABBITMQ_URL` | `auth`, `customer`, `inventory`, `notification`, `order`, `payment` |

Os defaults configurados em cada `application.yml` já são compatíveis entre si para uso local — você só precisa definir explicitamente em produção. As variáveis específicas de cada serviço estão no README dele.

---

## Ordem de subida recomendada

Como há dependências de chamadas Feign/WebClient entre os serviços, subir nesta ordem evita erros de conexão nos primeiros eventos:

1. **RabbitMQ** (serviço do Windows / Linux)
2. **auth** (`:8081`)
3. **customer** (`:8082`)
4. **inventory** (`:8083`)
5. **payment** (`:8086`)
6. **notification** (`:8084`)
7. **order** (`:8085`)
8. **assistant** (`:8087`)
9. **api-gateway** (`:8080`)
10. **ngrok** (`ngrok http 8086`) — e atualize o webhook no painel do AbacatePay com a URL nova

## Rodando um serviço

Dentro da pasta do serviço:

```bash
# Linux/macOS
./mvnw spring-boot:run
```

```powershell
# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

Cada README de serviço documenta as variáveis específicas e os endpoints expostos.

## Testes

Dentro de cada serviço:
```bash
./mvnw test
```

Serviços com teste E2E de ciclo de vida completo: `customer`, `order`, `payment`.

## Estrutura

```
accenture-project-api/
├── api-gateway/
├── assistant/
├── auth/
├── customer/
├── inventory/
├── notification/
├── order/
└── payment/
```

Cada pasta é um projeto Maven independente, com seu próprio `pom.xml`, `mvnw` e `src/`.
