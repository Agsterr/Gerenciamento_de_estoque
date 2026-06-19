# Documentação de Webhooks do Mercado Pago

> 📋 **Checklist de Produção:** Consulte [checklist-producao-webhooks.md](./checklist-producao-webhooks.md) antes de fazer deploy.

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Fluxo Completo do Webhook](#fluxo-completo-do-webhook)
3. [Exemplos de Payload](#exemplos-de-payload)
4. [Tabela de Status](#tabela-de-status)
5. [Regras de Negócio](#regras-de-negócio)
6. [Endpoints](#endpoints)
7. [Validação de Assinatura](#validação-de-assinatura)
8. [Idempotência](#idempotência)
9. [Tratamento de Erros](#tratamento-de-erros)

---

## 🎯 Visão Geral

O sistema processa webhooks do Mercado Pago para manter sincronização em tempo real com eventos de pagamento, assinaturas, cartões e chargebacks.

### Tipos de Eventos Suportados

- `payment` - Pagamentos aprovados, pendentes, rejeitados
- `merchant_order` - Ordens comerciais
- `chargebacks` - Estornos e disputas
- `subscriptions` - Assinaturas (preapproval, authorized_payment)
- `card.updated` - Atualização de cartões

---

## 🔄 Fluxo Completo do Webhook

```
┌─────────────────┐
│  Mercado Pago   │
│   (Webhook)     │
└────────┬────────┘
         │
         │ POST /webhooks/mercadopago
         │ Headers: x-signature, x-request-id
         │ Body: JSON payload
         ▼
┌─────────────────┐
│ WebhookController│
│  - Recebe webhook│
│  - Loga payload  │
│  - Retorna 200   │
└────────┬────────┘
         │
         │ Delegar processamento
         ▼
┌─────────────────┐
│ WebhookService  │
│  - Valida assinatura
│  - Verifica idempotência
│  - Identifica tipo de evento
└────────┬────────┘
         │
         │ Roteamento por tipo
         ▼
┌─────────────────────────────────────┐
│     WebhookEventHandler             │
│  (Strategy Pattern)                 │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ PaymentWebhookHandler         │  │
│  │ - Processa pagamentos        │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ SubscriptionWebhookHandler   │  │
│  │ - Processa assinaturas       │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ ChargebackWebhookHandler    │  │
│  │ - Processa chargebacks       │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ CardWebhookHandler           │  │
│  │ - Atualiza cartões           │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ MerchantOrderWebhookHandler  │  │
│  │ - Processa ordens            │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
         │
         │ Processamento específico
         ▼
┌─────────────────┐
│  Banco de Dados │
│  - Payment      │
│  - Subscription │
│  - CardHistory  │
│  - WebhookEvent │
└─────────────────┘
```

### Passos Detalhados

1. **Recepção**: `WebhookController` recebe POST com payload JSON
2. **Validação**: Verifica assinatura `x-signature` usando HMAC-SHA256
3. **Idempotência**: Verifica se evento já foi processado (`webhook_events`)
4. **Roteamento**: Identifica tipo de evento e delega ao handler apropriado
5. **Processamento**: Handler específico processa o evento
6. **Persistência**: Dados são salvos no banco de dados
7. **Resposta**: Retorna HTTP 200 imediatamente (processamento assíncrono)

---

## 📦 Exemplos de Payload

### 1. Evento de Pagamento (payment)

```json
{
  "id": "123456789",
  "live_mode": true,
  "type": "payment",
  "date_created": "2025-01-15T10:30:00Z",
  "application_id": "123456789",
  "user_id": "987654321",
  "version": 1,
  "api_version": "v1",
  "action": "payment.created",
  "data": {
    "id": "789012345"
  }
}
```

**Campos Importantes:**
- `type`: Sempre `"payment"` para eventos de pagamento
- `data.id`: ID do pagamento no Mercado Pago
- `action`: Ação do evento (created, updated, etc.)

### 2. Evento de Assinatura (subscriptions)

```json
{
  "id": "123456789",
  "live_mode": true,
  "type": "subscriptions",
  "date_created": "2025-01-15T10:30:00Z",
  "action": "subscription_preapproval",
  "data": {
    "id": "preapproval_123456"
  }
}
```

**Tipos de Action:**
- `subscription_preapproval`: Criação/atualização de assinatura
- `subscription_authorized_payment`: Pagamento autorizado para renovação

### 3. Evento de Chargeback (chargebacks)

```json
{
  "id": "123456789",
  "live_mode": true,
  "type": "chargebacks",
  "date_created": "2025-01-15T10:30:00Z",
  "action": "chargeback.created",
  "data": {
    "id": "chargeback_123456"
  }
}
```

### 4. Evento de Atualização de Cartão (card.updated)

```json
{
  "id": "123456789",
  "live_mode": true,
  "type": "card.updated",
  "date_created": "2025-01-15T10:30:00Z",
  "action": "card.updated",
  "data": {
    "id": "card_123456"
  }
}
```

### 5. Evento de Merchant Order (merchant_order)

```json
{
  "id": "123456789",
  "live_mode": true,
  "type": "merchant_order",
  "date_created": "2025-01-15T10:30:00Z",
  "action": "merchant_order.created",
  "data": {
    "id": "123456789"
  }
}
```

---

## 📊 Tabela de Status

### Status de Pagamento (PaymentStatus)

| Status Mercado Pago | Status Local | Descrição |
|---------------------|--------------|-----------|
| `approved` | `APPROVED` | Pagamento aprovado e confirmado |
| `pending` | `PENDING` | Pagamento pendente de confirmação |
| `in_process` | `PENDING` | Pagamento em processamento |
| `rejected` | `REJECTED` | Pagamento rejeitado |
| `cancelled` | `CANCELLED` | Pagamento cancelado |
| `canceled` | `CANCELLED` | Pagamento cancelado (alternativo) |
| `refunded` | `REFUNDED` | Pagamento reembolsado |
| `charged_back` | `CHARGED_BACK` | Estorno (chargeback) |

### Status de Assinatura (SubscriptionStatus)

| Status Mercado Pago | Status Local | Descrição |
|---------------------|--------------|-----------|
| `approved` | `ACTIVE` | Assinatura ativa |
| `authorized` | `ACTIVE` | Assinatura autorizada |
| `active` | `ACTIVE` | Assinatura ativa |
| `pending` | `INCOMPLETE` | Assinatura pendente |
| `in_process` | `INCOMPLETE` | Assinatura em processamento |
| `cancelled` | `CANCELED` | Assinatura cancelada |
| `canceled` | `CANCELED` | Assinatura cancelada (alternativo) |
| `paused` | `PAST_DUE` | Assinatura pausada |
| `rejected` | `PAST_DUE` | Assinatura rejeitada |
| `refunded` | `CANCELED` | Assinatura reembolsada |
| `charged_back` | `CANCELED` | Assinatura com chargeback |

### Status de Merchant Order

| Status | Descrição |
|--------|-----------|
| `PAGO` | Ordem totalmente paga |
| `PARCIAL` | Ordem parcialmente paga |
| `NÃO PAGO` | Ordem não paga |

---

## ⚙️ Regras de Negócio

### 1. Processamento de Pagamentos

- ✅ **Sempre buscar detalhes completos via API**: `GET /v1/payments/{id}`
- ✅ **NÃO confiar apenas no payload do webhook**
- ✅ **Idempotência**: Verificar `mercado_pago_payment_id` antes de criar
- ✅ **Vínculo obrigatório**: Todo `Payment` deve ter `Subscription`
- ✅ **Ativação condicional**: Só ativar assinatura se status = `APPROVED`

**Fluxo:**
```
1. Receber webhook payment
2. Validar assinatura
3. Verificar idempotência
4. Buscar pagamento completo via API
5. Buscar/criar Subscription
6. Criar/atualizar Payment
7. Se approved → Ativar Subscription
```

### 2. Processamento de Assinaturas

- ✅ **subscription_preapproval**: Criar/atualizar assinatura
- ✅ **subscription_authorized_payment**: Renovação mensal automática
- ✅ **Renovação mensal**: Atualizar `current_period_end` para +1 mês
- ✅ **Status**: Mapear corretamente conforme tabela acima

**Fluxo subscription_preapproval:**
```
1. Receber webhook
2. Extrair preference_id e metadata
3. Buscar Subscription existente ou criar nova
4. Atualizar status conforme action e status
5. Configurar períodos de renovação
```

**Fluxo subscription_authorized_payment:**
```
1. Receber webhook
2. Buscar Subscription
3. Buscar pagamento via API
4. Criar Payment vinculado
5. Se approved → Renovar período (+1 mês)
```

### 3. Processamento de Chargebacks

- ✅ **Atualizar Payment**: `status = CHARGED_BACK`
- ✅ **Suspender Subscription**: `status = PAST_DUE`
- ✅ **Registrar histórico**: Log detalhado
- ❌ **NÃO permitir reativação automática**: Requer intervenção manual

**Fluxo:**
```
1. Receber webhook chargeback
2. Buscar Payment por payment_id
3. Atualizar Payment.status = CHARGED_BACK
4. Suspender Subscription.status = PAST_DUE
5. Registrar histórico
6. Bloquear reativação automática
```

### 4. Processamento de Merchant Order

- ✅ **Buscar ordem via API**: `GET /merchant_orders/{id}`
- ✅ **Somar pagamentos aprovados**: Apenas status `approved`
- ✅ **Comparar com total**: `paidAmount >= totalAmount`
- ✅ **Liberação condicional**: Só liberar quando `PAGO`

**Fluxo:**
```
1. Receber webhook merchant_order
2. Buscar ordem via API
3. Calcular total da ordem
4. Somar pagamentos aprovados
5. Determinar status (PAGO, PARCIAL, NÃO PAGO)
6. Se PAGO → Liberar pedido
```

### 5. Processamento de Card Updated

- ✅ **Atualizar card_id**: Na tabela `subscriptions`
- ✅ **Manter histórico**: Tabela `card_history`
- ✅ **Garantir uso futuro**: Novos pagamentos usam novo card_id
- ✅ **Auditoria**: Log detalhado de alterações

**Fluxo:**
```
1. Receber webhook card.updated
2. Extrair card_id e customer_id
3. Buscar Subscription por customer_id
4. Salvar card_id antigo no histórico
5. Atualizar card_id na Subscription
6. Registrar em card_history
7. Logar para auditoria
```

---

## 🔗 Endpoints

### POST /webhooks/mercadopago

**Descrição**: Endpoint público para receber webhooks do Mercado Pago

**Headers Obrigatórios:**
- `x-signature`: Assinatura HMAC-SHA256 (formato: `ts=<timestamp>,v1=<hash>`)
- `x-request-id`: ID único da requisição (opcional, para idempotência)

**Body:**
```json
{
  "type": "payment",
  "data": {
    "id": "123456789"
  }
}
```

**Respostas:**
- `200 OK`: Webhook recebido e processado
- `401 Unauthorized`: Assinatura inválida
- `400 Bad Request`: Payload inválido
- `500 Internal Server Error`: Erro no processamento

### GET /admin/webhooks/failed

**Descrição**: Lista eventos de webhook que falharam (requer role ADMIN)

**Respostas:**
- `200 OK`: Lista de eventos falhados

### POST /admin/webhooks/failed/{eventId}/reprocess

**Descrição**: Reprocessa um evento falhado (requer role ADMIN)

**Respostas:**
- `200 OK`: Evento reprocessado com sucesso
- `404 Not Found`: Evento não encontrado
- `500 Internal Server Error`: Erro ao reprocessar

---

## 🔐 Validação de Assinatura

### Algoritmo

1. **Parse do header**: `x-signature = "ts=<timestamp>,v1=<hash>"`
2. **Construir payload assinado**: `signedPayload = ts + "." + requestBody`
3. **Calcular hash esperado**: `expectedHash = HMAC-SHA256(secret, signedPayload)`
4. **Comparar hashes**: Comparação timing-safe entre `expectedHash` e `v1`

### Exemplo

```java
String signature = "ts=1700000000,v1=abc123...";
String requestBody = "{\"type\":\"payment\",\"data\":{\"id\":\"123\"}}";
String secret = "webhook_secret_from_panel";

// 1. Parse
String ts = "1700000000";
String v1 = "abc123...";

// 2. Construir payload
String signedPayload = ts + "." + requestBody;

// 3. Calcular hash
String expectedHash = hmacSha256(secret, signedPayload);

// 4. Comparar (timing-safe)
boolean isValid = constantTimeEquals(expectedHash, v1);
```

### Rejeição

- ❌ Assinatura ausente → `401 Unauthorized`
- ❌ Hash inválido → `401 Unauthorized`
- ❌ Formato inválido → `401 Unauthorized`

---

## 🔄 Idempotência

### Mecanismo

1. **Tabela `webhook_events`**: Armazena `event_id` único
2. **Verificação**: `webhookEventRepository.existsByEventId(eventId)`
3. **Persistência**: Salvar ANTES de processar (garante idempotência mesmo em erro)
4. **Event ID**: Vem do campo `data.id` do payload

### Fluxo

```
1. Extrair eventId do payload (data.id)
2. Verificar se existe em webhook_events
3. Se existe → Retornar alreadyProcessed
4. Se não existe → Salvar em webhook_events
5. Processar evento
```

### Garantias

- ✅ **Mesmo evento não é processado duas vezes**
- ✅ **Idempotência persistente** (sobrevive a restarts)
- ✅ **Race condition**: Tratada com constraint UNIQUE

---

## 🚨 Tratamento de Erros

### Eventos Falhados

**Tabela `failed_webhook_events`:**
- Armazena payload completo
- Mensagem de erro
- Stack trace
- Contador de tentativas (`retry_count`)

### Fluxo de Erro

```
1. Erro no processamento
2. Salvar em failed_webhook_events
3. Logar erro detalhado
4. Retornar HTTP 200 (não afeta resposta)
5. Admin pode reprocessar via endpoint
```

### Reprocessamento

- **Endpoint**: `POST /admin/webhooks/failed/{eventId}/reprocess`
- **Incrementa**: `retry_count`
- **Atualiza**: `last_retry_at`
- **Loga**: Tentativa de reprocessamento

---

## 📝 Notas Importantes

1. **Sempre buscar dados completos via API**: Não confiar apenas no payload do webhook
2. **Validação obrigatória**: Assinatura deve ser validada ANTES de qualquer processamento
3. **Idempotência crítica**: Garantir que eventos não sejam processados duas vezes
4. **Vínculo obrigatório**: Payment sempre deve ter Subscription
5. **Ativação condicional**: Só ativar assinatura se pagamento aprovado
6. **Chargeback bloqueia reativação**: Requer intervenção manual
7. **Histórico de cartões**: Manter registro completo para auditoria
8. **Logs detalhados**: Todas as operações são logadas para auditoria

---

## 🛡️ Proteção Contra Webhooks Fora de Ordem

### Problema

Webhooks podem chegar fora de ordem devido a:
- Latência de rede
- Retries do Mercado Pago
- Processamento assíncrono
- Múltiplas instâncias do servidor

**Exemplo problemático:**
1. Webhook APPROVED chega primeiro (timestamp T1)
2. Webhook PENDING chega depois (timestamp T0, mais antigo)
3. ❌ Sistema regrediria status de APPROVED → PENDING (ERRADO!)

### Solução Implementada

**Campo `last_status_update_at` em Payment:**
- Armazena timestamp da última atualização de status processada
- Sempre baseado em `date_last_updated` da API do Mercado Pago (fonte da verdade)
- Usado para validar ordem temporal antes de atualizar status

**Regras de Negócio:**
1. ✅ **APPROVED nunca pode regredir** - Se payment está APPROVED, eventos mais antigos são ignorados
2. ✅ **Sempre vence o status mais recente** - Comparação baseada em timestamp da API
3. ✅ **Webhook é apenas gatilho** - API do Mercado Pago é a fonte da verdade
4. ✅ **Validação antes de atualizar** - Verifica `lastStatusUpdateAt` antes de processar

**Fluxo de Validação:**
```
1. Receber webhook
2. Buscar payment via API Mercado Pago
3. Extrair timestamp (date_last_updated)
4. Comparar com lastStatusUpdateAt do payment existente
5. Se evento é mais antigo → IGNORAR (log de warning)
6. Se evento é mais recente → PROCESSAR e atualizar lastStatusUpdateAt
```

**Exemplo de Log:**
```
⚠ Evento fora de ordem ignorado - paymentId=123456, statusAtual=APPROVED, 
  statusRecebido=PENDING, timestampAtual=2025-01-15T10:30:00, 
  timestampRecebido=2025-01-15T10:20:00
⚠ REGRA: Não permitir regressão de status. Status atual (APPROVED) é mais 
  recente que o recebido (PENDING).
```

---

## 🔄 Retry Automático de Webhooks

### Problema

Webhooks podem falhar durante processamento devido a:
- Restart/deploy do servidor
- Erros temporários (timeout, conexão)
- Falhas de validação
- Erros de negócio

**Consequência:** Eventos salvos mas não processados ficam perdidos.

### Solução Implementada

**Campos em `WebhookEvent`:**
- `processed` (boolean): Indica se evento foi completamente processado
- `processed_at` (timestamp): Quando foi processado com sucesso
- `error_message` (text): Mensagem de erro se falhou

**Job de Retry Automático (`WebhookRetryService`):**
- Executa a cada 5 minutos
- Busca eventos com `processed = false` criados há mais de 5 minutos
- Tenta reprocessar automaticamente
- Mantém idempotência (nunca reprocessa eventos já processados)

**Fluxo de Processamento:**
```
1. Receber webhook → Salvar event_id com processed = false
2. Processar evento
3. Se sucesso → Marcar processed = true, processed_at = now()
4. Se erro → Manter processed = false, salvar error_message
5. Job de retry → Reprocessa eventos não processados periodicamente
```

**Garantias:**
- ✅ **Idempotência**: Nunca reprocessa eventos com `processed = true`
- ✅ **Delay mínimo**: Aguarda 5 minutos antes de retry (evita conflito)
- ✅ **Logs detalhados**: Todas as tentativas são logadas
- ✅ **Métricas**: Estatísticas de eventos não processados disponíveis

**Endpoint de Retry Manual:**
```java
POST /admin/webhooks/{eventId}/reprocess
// Permite reprocessamento manual de eventos específicos
```

---

## 🔍 Fonte da Verdade: API vs Webhook

### ⚠️ IMPORTANTE: Webhook ≠ Fonte da Verdade

**Regra fundamental:**
> O webhook é apenas um **gatilho** para buscar dados atualizados na API do Mercado Pago.

**Por quê?**
- Webhooks podem chegar fora de ordem
- Webhooks podem ter dados desatualizados
- Webhooks podem ser duplicados
- API sempre retorna estado atualizado e confiável

**Fluxo Correto:**
```
1. Receber webhook (apenas notificação)
2. Extrair payment_id do webhook
3. Buscar payment completo via API: GET /v1/payments/{id}
4. Usar dados da API (não do webhook) para processar
5. Validar ordem temporal usando date_last_updated da API
```

**Campos Confiáveis da API:**
- `status` - Status atual do pagamento
- `date_last_updated` - Timestamp da última mudança (fonte da verdade para ordem)
- `date_approved` - Quando foi aprovado
- `metadata` - Metadados do pagamento

**Nunca confiar apenas em:**
- ❌ Status do payload do webhook
- ❌ Timestamp de chegada do webhook
- ❌ Ordem de chegada dos webhooks

---

## 🔗 Referências

- [Documentação Oficial Mercado Pago - Webhooks](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks)
- [Documentação Oficial Mercado Pago - Payments API](https://www.mercadopago.com.br/developers/pt/reference/payments/_payments_id/get)
- [Documentação Oficial Mercado Pago - Merchant Orders](https://www.mercadopago.com.br/developers/pt/reference/merchant_orders/_merchant_orders_id/get)

---

**Última atualização**: 2025-01-15
**Versão**: 1.0.0

