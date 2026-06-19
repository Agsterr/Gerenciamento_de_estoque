# 📘 MANUAL FRONT-END - INTEGRAÇÃO MERCADO PAGO

> **Documento completo para desenvolvedores Angular**  
> Última atualização: 2025-01-15

---

## 📋 ÍNDICE

1. [Autenticação e Segurança](#1-autenticação-e-segurança)
2. [Endpoints do Mercado Pago](#2-endpoints-do-mercado-pago)
3. [Status de Pagamento](#3-status-de-pagamento)
4. [Assinaturas (Planos/Recorrência)](#4-assinaturas-planosrecorrência)
5. [Dados Sensíveis](#5-dados-sensíveis)
6. [Erros e Mensagens](#6-erros-e-mensagens)
7. [Performance e UX](#7-performance-e-ux)
8. [Configurações Necessárias](#8-configurações-necessárias)
9. [O Que Não Existe e Precisa Ser Feito](#9-o-que-não-existe-e-precisa-ser-feito)
10. [Checklist Final](#10-checklist-final)

---

## 1️⃣ AUTENTICAÇÃO E SEGURANÇA

### ✅ Método de Autenticação

**O front-end usa JWT (JSON Web Token)**

### 📤 Header de Autenticação

O Angular deve enviar o seguinte header em TODAS as requisições:

```http
Authorization: Bearer <token>
```

**Exemplo:**
```typescript
headers: {
  'Authorization': `Bearer ${localStorage.getItem('jwtToken')}`
}
```

### 🔄 Refresh Token

**❌ NÃO EXISTE – PRECISA IMPLEMENTAR**

Atualmente o sistema não possui refresh token. O token JWT tem validade de **10 horas** (36000 segundos).

**Recomendação para implementação:**
- Implementar endpoint `/api/auth/refresh` que recebe o token atual e retorna um novo
- Front-end deve verificar expiração do token antes de fazer requisições
- Se token expirado, redirecionar para login

### 👥 Roles/Perfis que Afetam Pagamentos

**✅ EXISTE**

O sistema possui roles que podem afetar acesso a funcionalidades, mas **não bloqueiam diretamente pagamentos**. As roles são:

- `ROLE_ADMIN` - Acesso total
- `ROLE_USER` - Acesso padrão
- Outras roles customizadas por organização

**Importante:** O controle de acesso a recursos (relatórios, analytics, API) é baseado no **plano da assinatura**, não apenas nas roles.

---

## 2️⃣ ENDPOINTS DO MERCADO PAGO (CONTRATO PARA O FRONT)

### 📌 Endpoint 1: Criar Assinatura

```json
{
  "method": "POST",
  "url": "/api/subscription/create",
  "descricao": "Cria uma nova assinatura e retorna URL de checkout do Mercado Pago",
  "body": {},
  "queryParams": {
    "planId": "Long (obrigatório) - ID do plano selecionado"
  },
  "headersObrigatorios": [
    "Authorization: Bearer <token>"
  ],
  "respostaSucesso": {
    "checkoutUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=...",
    "sessionId": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=..."
  },
  "possiveisErros": [
    {
      "httpStatus": 400,
      "mensagem": "Usuário já possui uma assinatura ativa",
      "acaoFront": "Mostrar mensagem e redirecionar para página de assinatura atual"
    },
    {
      "httpStatus": 401,
      "mensagem": "Token inválido ou expirado",
      "acaoFront": "Redirecionar para login"
    },
    {
      "httpStatus": 404,
      "mensagem": "Plano não encontrado",
      "acaoFront": "Mostrar erro e permitir selecionar outro plano"
    },
    {
      "httpStatus": 500,
      "mensagem": "Erro ao criar assinatura no Mercado Pago",
      "acaoFront": "Mostrar mensagem de erro e permitir tentar novamente"
    }
  ]
}
```

**Exemplo de uso Angular:**
```typescript
createSubscription(planId: number): Observable<any> {
  return this.http.post<any>(
    `${this.apiUrl}/api/subscription/create?planId=${planId}`,
    {},
    { headers: this.getAuthHeaders() }
  );
}
```

---

### 📌 Endpoint 2: Consultar Assinatura Atual

```json
{
  "method": "GET",
  "url": "/api/subscription/current",
  "descricao": "Retorna a assinatura ativa do usuário autenticado",
  "body": {},
  "queryParams": {},
  "headersObrigatorios": [
    "Authorization: Bearer <token>"
  ],
  "respostaSucesso": {
    "id": 1,
    "userId": 123,
    "userEmail": "usuario@example.com",
    "userName": "João Silva",
    "planId": 1,
    "planName": "Plano Premium",
    "planType": "PREMIUM",
    "planPrice": 99.90,
    "status": "ACTIVE",
    "stripeSubscriptionId": "pref_123456789",
    "trialStart": "2025-01-01T00:00:00",
    "trialEnd": "2025-01-15T00:00:00",
    "currentPeriodStart": "2025-01-15T00:00:00",
    "currentPeriodEnd": "2025-02-15T00:00:00",
    "canceledAt": null,
    "endedAt": null,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-15T00:00:00",
    "isInTrial": false,
    "isTrialEndingSoon": false,
    "isActive": true,
    "maxUsers": 10,
    "maxProducts": 1000,
    "maxOrganizations": 1,
    "hasReports": true,
    "hasAdvancedAnalytics": true,
    "hasApiAccess": true
  },
  "possiveisErros": [
    {
      "httpStatus": 404,
      "mensagem": "Assinatura não encontrada",
      "acaoFront": "Mostrar opção para criar nova assinatura"
    },
    {
      "httpStatus": 401,
      "mensagem": "Token inválido ou expirado",
      "acaoFront": "Redirecionar para login"
    }
  ]
}
```

---

### 📌 Endpoint 3: Cancelar Assinatura

```json
{
  "method": "POST",
  "url": "/api/subscription/cancel",
  "descricao": "Cancela a assinatura atual do usuário",
  "body": {},
  "queryParams": {},
  "headersObrigatorios": [
    "Authorization: Bearer <token>"
  ],
  "respostaSucesso": {
    "message": "Assinatura cancelada com sucesso"
  },
  "possiveisErros": [
    {
      "httpStatus": 400,
      "mensagem": "Usuário não possui assinatura ativa",
      "acaoFront": "Mostrar mensagem informativa"
    },
    {
      "httpStatus": 401,
      "mensagem": "Token inválido ou expirado",
      "acaoFront": "Redirecionar para login"
    },
    {
      "httpStatus": 500,
      "mensagem": "Erro ao cancelar assinatura no Mercado Pago",
      "acaoFront": "Mostrar erro e permitir tentar novamente"
    }
  ]
}
```

---

### 📌 Endpoint 4: Portal do Cliente (Gerenciar Assinatura)

```json
{
  "method": "GET",
  "url": "/api/subscription/customer-portal",
  "descricao": "Retorna URL do portal do cliente Mercado Pago para gerenciar assinatura",
  "body": {},
  "queryParams": {},
  "headersObrigatorios": [
    "Authorization: Bearer <token>"
  ],
  "respostaSucesso": {
    "url": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=..."
  },
  "possiveisErros": [
    {
      "httpStatus": 400,
      "mensagem": "Usuário não possui assinatura ativa",
      "acaoFront": "Mostrar mensagem e opção para criar assinatura"
    },
    {
      "httpStatus": 401,
      "mensagem": "Token inválido ou expirado",
      "acaoFront": "Redirecionar para login"
    }
  ]
}
```

---

### 📌 Endpoint 5: Histórico de Assinaturas

```json
{
  "method": "GET",
  "url": "/api/subscription/history",
  "descricao": "Retorna histórico de todas as assinaturas do usuário",
  "body": {},
  "queryParams": {},
  "headersObrigatorios": [
    "Authorization: Bearer <token>"
  ],
  "respostaSucesso": [
    {
      "id": 1,
      "userId": 123,
      "planName": "Plano Premium",
      "planPrice": 99.90,
      "status": "ACTIVE",
      "currentPeriodStart": "2025-01-15T00:00:00",
      "currentPeriodEnd": "2025-02-15T00:00:00",
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "possiveisErros": [
    {
      "httpStatus": 401,
      "mensagem": "Token inválido ou expirado",
      "acaoFront": "Redirecionar para login"
    }
  ]
}
```

---

### 📌 Endpoint 6: Verificar Acesso a Funcionalidade

```json
{
  "method": "GET",
  "url": "/api/subscription/feature-access",
  "descricao": "Verifica se o usuário tem acesso a uma funcionalidade específica baseado no plano",
  "body": {},
  "queryParams": {
    "feature": "String (obrigatório) - Nome da funcionalidade: 'reports', 'analytics', 'api_access'"
  },
  "headersObrigatorios": [
    "Authorization: Bearer <token>"
  ],
  "respostaSucesso": {
    "hasAccess": true
  },
  "possiveisErros": [
    {
      "httpStatus": 401,
      "mensagem": "Token inválido ou expirado",
      "acaoFront": "Redirecionar para login"
    }
  ]
}
```

---

### 📌 Endpoint 7: Verificar Limites de Uso

```json
{
  "method": "GET",
  "url": "/api/subscription/usage-limits",
  "descricao": "Verifica se o usuário está dentro dos limites do plano",
  "body": {},
  "queryParams": {
    "limitType": "String (obrigatório) - Tipo de limite: 'users', 'products', 'organizations'",
    "currentCount": "Number (obrigatório) - Quantidade atual"
  },
  "headersObrigatorios": [
    "Authorization: Bearer <token>"
  ],
  "respostaSucesso": {
    "withinLimits": true,
    "currentCount": 5
  },
  "possiveisErros": [
    {
      "httpStatus": 401,
      "mensagem": "Token inválido ou expirado",
      "acaoFront": "Redirecionar para login"
    }
  ]
}
```

---

### 📌 Endpoint 8: Listar Planos Disponíveis

```json
{
  "method": "GET",
  "url": "/api/plans",
  "descricao": "Lista todos os planos ativos disponíveis",
  "body": {},
  "queryParams": {},
  "headersObrigatorios": [
    "Authorization: Bearer <token>"
  ],
  "respostaSucesso": [
    {
      "id": 1,
      "name": "Plano Básico",
      "description": "Plano básico para pequenas empresas",
      "price": 29.90,
      "type": "BASIC",
      "maxUsers": 3,
      "maxProducts": 100,
      "maxOrganizations": 1,
      "hasReports": false,
      "hasAdvancedAnalytics": false,
      "hasApiAccess": false
    },
    {
      "id": 2,
      "name": "Plano Premium",
      "description": "Plano completo com todos os recursos",
      "price": 99.90,
      "type": "PREMIUM",
      "maxUsers": 10,
      "maxProducts": 1000,
      "maxOrganizations": 1,
      "hasReports": true,
      "hasAdvancedAnalytics": true,
      "hasApiAccess": true
    }
  ],
  "possiveisErros": [
    {
      "httpStatus": 401,
      "mensagem": "Token inválido ou expirado",
      "acaoFront": "Redirecionar para login"
    }
  ]
}
```

---

### 📌 Endpoint 9: Buscar Plano por ID

```json
{
  "method": "GET",
  "url": "/api/plans/{id}",
  "descricao": "Busca detalhes de um plano específico",
  "body": {},
  "queryParams": {},
  "headersObrigatorios": [
    "Authorization: Bearer <token>"
  ],
  "respostaSucesso": {
    "id": 1,
    "name": "Plano Básico",
    "description": "Plano básico para pequenas empresas",
    "price": 29.90,
    "type": "BASIC",
    "maxUsers": 3,
    "maxProducts": 100,
    "maxOrganizations": 1,
    "hasReports": false,
    "hasAdvancedAnalytics": false,
    "hasApiAccess": false
  },
  "possiveisErros": [
    {
      "httpStatus": 404,
      "mensagem": "Plano não encontrado",
      "acaoFront": "Mostrar erro e redirecionar para lista de planos"
    },
    {
      "httpStatus": 401,
      "mensagem": "Token inválido ou expirado",
      "acaoFront": "Redirecionar para login"
    }
  ]
}
```

---

### ⚠️ IMPORTANTE: Endpoints que NÃO EXISTEM

**❌ NÃO EXISTE – PRECISA IMPLEMENTAR:**

1. **Consultar Status de Pagamento Específico**
   - Endpoint: `GET /api/payments/{paymentId}`
   - Descrição: Consultar status de um pagamento específico
   - **Ação:** Criar endpoint no back-end

2. **Listar Pagamentos da Assinatura**
   - Endpoint: `GET /api/subscription/payments`
   - Descrição: Listar todos os pagamentos de uma assinatura
   - **Ação:** Criar endpoint no back-end

3. **Criar Link de Pagamento Único (não recorrente)**
   - Endpoint: `POST /api/payments/create-link`
   - Descrição: Criar link de pagamento único (não para assinatura)
   - **Ação:** Criar endpoint no back-end se necessário

---

## 3️⃣ STATUS DE PAGAMENTO (MAPEAMENTO)

### 📊 Status de Pagamento Retornados pelo Back-end

```json
{
  "APPROVED": {
    "codigo": "APPROVED",
    "descricao": "Pagamento aprovado e confirmado",
    "acaoFront": "Mostrar sucesso, ativar acesso ao produto"
  },
  "PENDING": {
    "codigo": "PENDING",
    "descricao": "Pagamento pendente de confirmação",
    "acaoFront": "Mostrar mensagem de aguardo, permitir verificar status"
  },
  "REJECTED": {
    "codigo": "REJECTED",
    "descricao": "Pagamento rejeitado",
    "acaoFront": "Mostrar erro, permitir tentar novamente"
  },
  "CANCELLED": {
    "codigo": "CANCELLED",
    "descricao": "Pagamento cancelado",
    "acaoFront": "Mostrar mensagem, permitir criar nova assinatura"
  },
  "REFUNDED": {
    "codigo": "REFUNDED",
    "descricao": "Pagamento reembolsado",
    "acaoFront": "Mostrar mensagem, desativar acesso"
  },
  "CHARGED_BACK": {
    "codigo": "CHARGED_BACK",
    "descricao": "Estorno (chargeback) - acesso bloqueado",
    "acaoFront": "Mostrar alerta crítico, bloquear acesso imediatamente"
  }
}
```

### ✅ Confirmação: Front deve confiar nesse status?

**SIM, o front deve confiar no status retornado pelo back-end.**

O back-end:
- ✅ Busca status atualizado diretamente da API do Mercado Pago
- ✅ Valida assinatura de webhooks
- ✅ Garante idempotência
- ✅ Previne regressão de status

### ❌ Front NÃO deve falar direto com o Mercado Pago?

**CORRETO. O front NUNCA deve acessar a API do Mercado Pago diretamente.**

**Razões:**
- 🔒 Segurança: Access Token nunca deve ir para o front
- 🎯 Controle: Back-end é a única fonte da verdade
- 🔄 Sincronização: Webhooks atualizam status automaticamente
- 📊 Auditoria: Todas as transações são registradas no back-end

---

## 4️⃣ ASSINATURAS (PLANO / RECORRÊNCIA)

### 📋 Criação de Plano

**✅ O plano é criado VIA API (back-end)**

Os planos são criados e gerenciados pelo back-end através do endpoint `/api/plans/sync-mercadopago`.

### 📤 O que o Front Precisa Enviar

**Para criar assinatura:**
- ✅ `planId` (Long) - ID do plano selecionado
- ❌ `preapprovalId` - NÃO é necessário (gerado pelo back-end)

### 🔄 Fluxo Correto

```
Front → Back → Mercado Pago
```

**Passo a passo:**
1. Front chama `POST /api/subscription/create?planId={id}`
2. Back cria Preference no Mercado Pago
3. Back retorna `checkoutUrl` para o front
4. Front redireciona usuário para `checkoutUrl`
5. Usuário completa pagamento no Mercado Pago
6. Mercado Pago redireciona para URLs configuradas (success/cancel/pending)
7. Webhook atualiza status no back-end automaticamente

### 📥 O que o Front Recebe

**Ao criar assinatura:**
```json
{
  "checkoutUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=...",
  "sessionId": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=..."
}
```

**Status da assinatura:**
- Consultar via `GET /api/subscription/current`
- Status possíveis: `TRIAL`, `ACTIVE`, `CANCELED`, `PAUSED`, `PAST_DUE`, `EXPIRED`, `INCOMPLETE`, `INCOMPLETE_EXPIRED`

---

## 5️⃣ DADOS SENSÍVEIS

### 🚫 O Front NUNCA Recebe

```json
{
  "proibidos": [
    "Access Token do Mercado Pago",
    "Webhook Secret",
    "Chaves privadas",
    "Credenciais de API",
    "Senhas",
    "Tokens de autenticação do Mercado Pago"
  ]
}
```

### ✅ O Front PODE Receber

```json
{
  "permitidos": [
    "checkoutUrl (URL pública do Mercado Pago)",
    "sessionId (mesmo que checkoutUrl)",
    "Status de pagamento (APPROVED, PENDING, etc.)",
    "Status de assinatura (ACTIVE, TRIAL, etc.)",
    "IDs de assinatura (stripeSubscriptionId - que na verdade é preference_id)",
    "Dados do plano (nome, preço, recursos)",
    "Datas (trialStart, trialEnd, currentPeriodStart, etc.)",
    "Limites do plano (maxUsers, maxProducts, etc.)"
  ]
}
```

---

## 6️⃣ ERROS E MENSAGENS

### 📋 Erros que o Front Deve Tratar

```json
[
  {
    "codigo": "SUBSCRIPTION_ALREADY_EXISTS",
    "httpStatus": 400,
    "mensagem": "Usuário já possui uma assinatura ativa",
    "acaoFront": "Mostrar mensagem e redirecionar para página de assinatura atual"
  },
  {
    "codigo": "PLAN_NOT_FOUND",
    "httpStatus": 404,
    "mensagem": "Plano não encontrado",
    "acaoFront": "Mostrar erro e permitir selecionar outro plano"
  },
  {
    "codigo": "SUBSCRIPTION_NOT_FOUND",
    "httpStatus": 404,
    "mensagem": "Assinatura não encontrada",
    "acaoFront": "Mostrar opção para criar nova assinatura"
  },
  {
    "codigo": "PAYMENT_REJECTED",
    "httpStatus": 400,
    "mensagem": "Pagamento recusado",
    "acaoFront": "Mostrar mensagem ao usuário, permitir tentar novamente"
  },
  {
    "codigo": "PAYMENT_PENDING",
    "httpStatus": 200,
    "mensagem": "Pagamento pendente de confirmação",
    "acaoFront": "Mostrar mensagem de aguardo, implementar polling opcional"
  },
  {
    "codigo": "MERCADOPAGO_ERROR",
    "httpStatus": 500,
    "mensagem": "Erro ao processar no Mercado Pago",
    "acaoFront": "Mostrar mensagem de erro genérica, permitir tentar novamente"
  },
  {
    "codigo": "UNAUTHORIZED",
    "httpStatus": 401,
    "mensagem": "Token inválido ou expirado",
    "acaoFront": "Redirecionar para login, limpar localStorage"
  },
  {
    "codigo": "FORBIDDEN",
    "httpStatus": 403,
    "mensagem": "Acesso negado",
    "acaoFront": "Mostrar mensagem de permissão negada"
  },
  {
    "codigo": "CHARGEBACK_DETECTED",
    "httpStatus": 200,
    "mensagem": "Chargeback detectado - acesso bloqueado",
    "acaoFront": "Mostrar alerta crítico, bloquear acesso imediatamente, contatar suporte"
  }
]
```

---

## 7️⃣ PERFORMANCE E UX

### ⚡ Endpoints Assíncronos

**✅ NÃO há endpoints assíncronos que retornam job ID**

Todos os endpoints são síncronos e retornam resposta imediata.

### 🔄 Polling

**❌ NÃO é necessário fazer polling**

O sistema usa webhooks para atualizar status automaticamente. O front pode:

1. **Consultar status após redirecionamento do Mercado Pago:**
   - Após usuário voltar de `successUrl`, consultar `GET /api/subscription/current`
   - Status será atualizado via webhook em tempo real

2. **Verificar status periodicamente (opcional):**
   - Se necessário, fazer polling a cada 5-10 segundos após criar assinatura
   - Parar polling quando status mudar para `ACTIVE` ou `REJECTED`

### 📊 Endpoint para Consultar Status Atualizado

**✅ EXISTE**

```http
GET /api/subscription/current
```

Retorna status atualizado da assinatura, incluindo:
- Status da assinatura
- Período atual
- Informações do plano
- Datas de trial

---

## 8️⃣ CONFIGURAÇÕES NECESSÁRIAS NO FRONT

### 🔧 Variáveis de Ambiente Angular

```typescript
// environment.ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080', // ou URL de produção
  timeoutPagamento: 30000, // 30 segundos
  pollingInterval: 5000, // 5 segundos (opcional)
  mercadoPagoSuccessUrl: 'http://localhost:4200/subscription/success',
  mercadoPagoCancelUrl: 'http://localhost:4200/subscription/cancel',
  mercadoPagoPendingUrl: 'http://localhost:4200/subscription/pending'
};
```

### 📝 Variáveis Necessárias

| Variável | Tipo | Descrição | Exemplo |
|----------|------|-----------|---------|
| `API_BASE_URL` | String | URL base da API | `http://localhost:8080` |
| `TIMEOUT_PAGAMENTO` | Number | Timeout para requisições de pagamento (ms) | `30000` |
| `POLLING_INTERVAL` | Number | Intervalo para polling opcional (ms) | `5000` |
| `MERCADOPAGO_SUCCESS_URL` | String | URL de retorno após pagamento aprovado | `/subscription/success` |
| `MERCADOPAGO_CANCEL_URL` | String | URL de retorno após cancelamento | `/subscription/cancel` |
| `MERCADOPAGO_PENDING_URL` | String | URL de retorno para pagamento pendente | `/subscription/pending` |

---

## 9️⃣ O QUE NÃO EXISTE E PRECISA SER FEITO

### ❌ Endpoints Faltantes

- [ ] **Endpoint para consultar pagamento específico**
  - `GET /api/payments/{paymentId}`
  - Retornar detalhes de um pagamento específico

- [ ] **Endpoint para listar pagamentos da assinatura**
  - `GET /api/subscription/payments`
  - Retornar histórico de pagamentos da assinatura atual

- [ ] **Endpoint para criar link de pagamento único**
  - `POST /api/payments/create-link`
  - Criar link de pagamento não recorrente (se necessário)

- [ ] **Endpoint de refresh token**
  - `POST /api/auth/refresh`
  - Renovar token JWT sem precisar fazer login novamente

### ❌ Padronização Faltante

- [ ] **DTO específico para respostas de pagamento**
  - Criar `PaymentDto` com estrutura padronizada
  - Incluir todos os campos relevantes

- [ ] **Códigos de erro padronizados**
  - Implementar códigos de erro consistentes
  - Documentar todos os códigos possíveis

- [ ] **Validação de status no front**
  - Criar enum TypeScript para status de pagamento
  - Criar enum TypeScript para status de assinatura

### ❌ Funcionalidades Faltantes

- [ ] **Página de sucesso de pagamento**
  - Criar componente Angular para `/subscription/success`
  - Mostrar confirmação e redirecionar após alguns segundos

- [ ] **Página de cancelamento de pagamento**
  - Criar componente Angular para `/subscription/cancel`
  - Permitir tentar novamente

- [ ] **Página de pagamento pendente**
  - Criar componente Angular para `/subscription/pending`
  - Mostrar instruções e permitir verificar status

- [ ] **Componente de gerenciamento de assinatura**
  - Mostrar status atual
  - Permitir cancelar assinatura
  - Mostrar histórico de pagamentos
  - Acessar portal do cliente

- [ ] **Guarda de rota para verificar assinatura ativa**
  - Criar `SubscriptionGuard` que verifica se usuário tem assinatura ativa
  - Bloquear acesso a funcionalidades premium se não tiver assinatura

- [ ] **Serviço Angular para gerenciar assinaturas**
  - Criar `SubscriptionService` no Angular
  - Métodos para criar, cancelar, consultar assinatura
  - Cache de status de assinatura

---

## 🔟 CHECKLIST FINAL (OBRIGATÓRIO)

### ✅ Back-end para Front-end

- [x] **Endpoints documentados**
  - ✅ Endpoints de assinatura documentados
  - ✅ Endpoints de planos documentados
  - ⚠️ Endpoints de pagamento específicos faltando

- [x] **Status padronizados**
  - ✅ Status de pagamento mapeados
  - ✅ Status de assinatura mapeados
  - ✅ Enums definidos no back-end

- [x] **Segurança validada**
  - ✅ JWT implementado
  - ✅ Headers de autenticação definidos
  - ⚠️ Refresh token não implementado

- [x] **Front não acessa MP direto**
  - ✅ Confirmado: front NUNCA deve acessar API do Mercado Pago
  - ✅ Todos os dados vêm do back-end

- [x] **Logs e idempotência OK**
  - ✅ Webhooks com idempotência
  - ✅ Logs estruturados
  - ✅ Validação de assinatura de webhooks

### 📋 Checklist de Implementação Front-end

- [ ] **Configurar variáveis de ambiente**
  - [ ] Definir `API_BASE_URL`
  - [ ] Definir `TIMEOUT_PAGAMENTO`
  - [ ] Definir URLs de retorno do Mercado Pago

- [ ] **Criar serviços Angular**
  - [ ] `SubscriptionService` - gerenciar assinaturas
  - [ ] `PlanService` - listar e consultar planos
  - [ ] `PaymentService` - consultar pagamentos (quando endpoint existir)

- [ ] **Criar componentes Angular**
  - [ ] `SubscriptionListComponent` - listar planos disponíveis
  - [ ] `SubscriptionDetailComponent` - detalhes da assinatura atual
  - [ ] `SubscriptionSuccessComponent` - página de sucesso
  - [ ] `SubscriptionCancelComponent` - página de cancelamento
  - [ ] `SubscriptionPendingComponent` - página de pagamento pendente
  - [ ] `SubscriptionManageComponent` - gerenciar assinatura

- [ ] **Criar guards e interceptors**
  - [ ] `SubscriptionGuard` - verificar assinatura ativa
  - [ ] Verificar se `AuthInterceptor` está configurado corretamente

- [ ] **Criar modelos TypeScript**
  - [ ] `Subscription` - modelo de assinatura
  - [ ] `Plan` - modelo de plano
  - [ ] `Payment` - modelo de pagamento
  - [ ] Enums para status (`PaymentStatus`, `SubscriptionStatus`)

- [ ] **Implementar tratamento de erros**
  - [ ] Tratar todos os erros documentados
  - [ ] Mostrar mensagens amigáveis ao usuário
  - [ ] Implementar retry para erros temporários

- [ ] **Implementar fluxo de checkout**
  - [ ] Selecionar plano
  - [ ] Chamar endpoint de criação de assinatura
  - [ ] Redirecionar para URL do Mercado Pago
  - [ ] Processar retorno (success/cancel/pending)
  - [ ] Verificar status atualizado

- [ ] **Implementar polling opcional**
  - [ ] Polling após criar assinatura (opcional)
  - [ ] Parar polling quando status mudar

- [ ] **Testes**
  - [ ] Testar criação de assinatura
  - [ ] Testar cancelamento de assinatura
  - [ ] Testar consulta de status
  - [ ] Testar tratamento de erros
  - [ ] Testar fluxo completo de checkout

---

## 📚 EXEMPLOS DE CÓDIGO ANGULAR

### Exemplo 1: Serviço de Assinatura

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SubscriptionService {
  private apiUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  createSubscription(planId: number): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/api/subscription/create?planId=${planId}`,
      {},
      { headers: this.getAuthHeaders() }
    );
  }

  getCurrentSubscription(): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/api/subscription/current`,
      { headers: this.getAuthHeaders() }
    );
  }

  cancelSubscription(): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/api/subscription/cancel`,
      {},
      { headers: this.getAuthHeaders() }
    );
  }

  getCustomerPortal(): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/api/subscription/customer-portal`,
      { headers: this.getAuthHeaders() }
    );
  }

  getSubscriptionHistory(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/api/subscription/history`,
      { headers: this.getAuthHeaders() }
    );
  }

  checkFeatureAccess(feature: string): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/api/subscription/feature-access?feature=${feature}`,
      { headers: this.getAuthHeaders() }
    );
  }

  checkUsageLimits(limitType: string, currentCount: number): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/api/subscription/usage-limits?limitType=${limitType}&currentCount=${currentCount}`,
      { headers: this.getAuthHeaders() }
    );
  }
}
```

### Exemplo 2: Modelos TypeScript

```typescript
// subscription.model.ts
export enum SubscriptionStatus {
  TRIAL = 'TRIAL',
  ACTIVE = 'ACTIVE',
  CANCELED = 'CANCELED',
  PAUSED = 'PAUSED',
  PAST_DUE = 'PAST_DUE',
  EXPIRED = 'EXPIRED',
  INCOMPLETE = 'INCOMPLETE',
  INCOMPLETE_EXPIRED = 'INCOMPLETE_EXPIRED'
}

export interface Subscription {
  id: number;
  userId: number;
  userEmail: string;
  userName: string;
  planId: number;
  planName: string;
  planType: string;
  planPrice: number;
  status: SubscriptionStatus;
  stripeSubscriptionId: string;
  trialStart: string;
  trialEnd: string;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  canceledAt: string | null;
  endedAt: string | null;
  createdAt: string;
  updatedAt: string;
  isInTrial: boolean;
  isTrialEndingSoon: boolean;
  isActive: boolean;
  maxUsers: number;
  maxProducts: number;
  maxOrganizations: number;
  hasReports: boolean;
  hasAdvancedAnalytics: boolean;
  hasApiAccess: boolean;
}

// payment.model.ts
export enum PaymentStatus {
  APPROVED = 'APPROVED',
  PENDING = 'PENDING',
  REJECTED = 'REJECTED',
  CANCELLED = 'CANCELLED',
  REFUNDED = 'REFUNDED',
  CHARGED_BACK = 'CHARGED_BACK'
}

export interface Payment {
  id: number;
  subscriptionId: number;
  amount: number;
  status: PaymentStatus;
  currency: string;
  paymentMethod: string;
  paidAt: string | null;
  failedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

// plan.model.ts
export interface Plan {
  id: number;
  name: string;
  description: string;
  price: number;
  type: string;
  maxUsers: number | null;
  maxProducts: number | null;
  maxOrganizations: number | null;
  hasReports: boolean;
  hasAdvancedAnalytics: boolean;
  hasApiAccess: boolean;
}
```

### Exemplo 3: Componente de Criação de Assinatura

```typescript
import { Component, OnInit } from '@angular/core';
import { SubscriptionService } from '../services/subscription.service';
import { PlanService } from '../services/plan.service';
import { Plan } from '../models/plan.model';

@Component({
  selector: 'app-subscription-create',
  templateUrl: './subscription-create.component.html'
})
export class SubscriptionCreateComponent implements OnInit {
  plans: Plan[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private subscriptionService: SubscriptionService,
    private planService: PlanService
  ) {}

  ngOnInit() {
    this.loadPlans();
  }

  loadPlans() {
    this.planService.getAllPlans().subscribe({
      next: (plans) => {
        this.plans = plans;
      },
      error: (err) => {
        this.error = 'Erro ao carregar planos';
        console.error(err);
      }
    });
  }

  selectPlan(planId: number) {
    this.loading = true;
    this.error = null;

    this.subscriptionService.createSubscription(planId).subscribe({
      next: (response) => {
        // Redirecionar para URL do Mercado Pago
        if (response.checkoutUrl) {
          window.location.href = response.checkoutUrl;
        }
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 400) {
          this.error = 'Você já possui uma assinatura ativa';
        } else if (err.status === 404) {
          this.error = 'Plano não encontrado';
        } else {
          this.error = 'Erro ao criar assinatura. Tente novamente.';
        }
        console.error(err);
      }
    });
  }
}
```

---

## 🎯 CONCLUSÃO

Este manual fornece todas as informações necessárias para implementar a integração com Mercado Pago no front-end Angular. 

**Próximos passos:**
1. Revisar este documento completamente
2. Implementar serviços Angular conforme exemplos
3. Criar componentes de UI para gerenciamento de assinaturas
4. Implementar tratamento de erros
5. Testar fluxo completo de checkout
6. Implementar funcionalidades faltantes listadas na seção 9

**Dúvidas?** Consulte a documentação do back-end ou entre em contato com a equipe de desenvolvimento.

---

**Documento gerado automaticamente em:** 2025-01-15  
**Versão:** 1.0.0







