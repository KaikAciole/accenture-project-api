# Accenture Store

E-commerce de cliente final (React + Vite + TypeScript + MUI) que conversa com o API Gateway em `http://localhost:8080`.

## Pré-requisitos
- Node.js 18+
- Yarn
- Backend rodando em `http://localhost:8080`

## Como rodar
```bash
yarn install
yarn dev
```

App em `http://localhost:5173`.

## Build
```bash
yarn build
```

## Variáveis de ambiente

Crie um `.env` na raiz (ou copie de `.env.example`):

```
VITE_API_BASE_URL=http://localhost:8080
VITE_MERCADO_PAGO_PUBLIC_KEY=TEST-xxx-yyy-zzz
```

## Credenciais de seed

Após o backend subir, rode `POST /api/v1/gateway/seed-data` (sem body, sem auth). Usuários criados:

- **Admin** — `admin@accestore.com` / `Admin@123`
- **Maria** (cliente) — `maria@accestore.com` / `Senha@123` — tem 1 pedido pré-criado e saldo na carteira
- **João** (cliente) — `joao@accestore.com` / `Senha@123`
- **Carlos** (cliente) — `carlos@accestore.com` / `Senha@123`
- 8 produtos cadastrados com `imageUrl` placeholder

A tela de login já mostra as credenciais de admin e Maria como dica.

## Fluxo de compra (do carrinho ao pedido pago)

1. Usuário adiciona itens ao carrinho (localStorage)
2. Em `/cart` clica em **Finalizar pedido** → vai para `/checkout`
3. Em `/checkout`:
   - Seleciona um endereço existente ou cadastra um novo (CEP em 2 passos)
   - Visualiza o resumo e o saldo da carteira
   - Clica em **Confirmar e pagar**
4. Frontend faz `POST /orders` com `{ addressId, items }`
5. Faz polling em `GET /orders/{id}` até status `RESERVED` (max 10s)
6. Faz `POST /payments` (`WALLET`) + `PATCH /payments/{id}/process`
7. Limpa carrinho e redireciona para `/orders/{id}`

## Recarregar carteira (Mercado Pago)

Em `/wallet` clique em **Recarregar saldo**:
1. Informa o valor → `POST /wallets/{walletId}/top-ups`
2. Backend retorna `clientToken` → frontend renderiza o Brick do Mercado Pago
3. Após o webhook confirmar, o saldo é atualizado (a tela faz polling do saldo a cada 3s enquanto o Dialog está aberto)

## Painel admin (`/admin/*`)

Visível apenas para usuários com role `ADMIN` (entrada "Admin" aparece no menu do usuário). Rotas:

- `/admin` — Dashboard (total de pedidos, ticket médio, saldo da empresa)
- `/admin/products` — CRUD de produtos
- `/admin/orders` — Lista global de pedidos
- `/admin/customers` — Lista de clientes (ver extrato em drawer, excluir)
- `/admin/company-wallet` — Saldo e extrato da carteira da empresa

`<AdminRoute>` bloqueia não-admins com Snackbar e redireciona para `/`.

## Stack
- React 18 + Vite 5 + TypeScript 6 (strict)
- `@mui/material` + `@mui/icons-material` + Emotion
- `react-router-dom` v6
- `jwt-decode`
- Fetch nativo via wrapper `src/api/client.ts`
- Context API (`AuthContext`, `CartContext`, `ThemeModeContext`, `SnackbarContext`)
- Mercado Pago SDK v2 (carregado via `<script>` no `index.html`)

## Convenções
- Sem axios, Redux, Tailwind ou styled-components — tudo MUI + `sx`
- Cor primária `#A100FF` (Accenture); tema claro/escuro com persistência (`localStorage.themeMode`)
- Toda chamada HTTP passa por `apiFetch<T>()`; nunca `fetch` direto
- Tipos do backend tolerantes (`?` + fallbacks `??`)
- JWT em cookie `access_token`; user em `localStorage`
- Carrinho em `localStorage` (chave `cart`)
- UI em português, código em inglês
