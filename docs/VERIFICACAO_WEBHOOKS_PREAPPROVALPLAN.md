# ✅ VERIFICAÇÃO E CORREÇÃO DE WEBHOOKS - PREAPPROVALPLAN

> **Documento técnico**  
> **Data:** 2025-01-15  
> **Status:** ✅ CORRIGIDO

---

## 📋 RESUMO

Os webhooks foram **verificados e corrigidos** para trabalhar corretamente com **PreapprovalPlan** (assinaturas recorrentes).

---

## ✅ O QUE FOI VERIFICADO

### 1. **Eventos de Webhook Tratados**

| Evento | Handler | Status |
|--------|---------|--------|
| `subscription_preapproval` | `SubscriptionWebhookHandler` | ✅ **CORRETO** |
| `subscription_authorized_payment` | `SubscriptionWebhookHandler` | ✅ **CORRETO** |
| `payment` | `PaymentWebhookHandler` | ✅ **CORRETO** |

### 2. **Fluxo de Processamento**

#### ✅ **Criação de Assinatura (`subscription_preapproval`)**

```
1. Webhook recebe evento subscription_preapproval
2. SubscriptionWebhookHandler.process() é chamado
3. Extrai preapproval_id do payload
4. Busca assinatura existente ou cria nova
5. Atualiza status baseado em action e status do preapproval
6. Configura período de renovação mensal
7. Salva assinatura no banco
```

**Status:** ✅ **FUNCIONANDO CORRETAMENTE**

#### ✅ **Renovação Mensal (`subscription_authorized_payment`)**

```
1. Webhook recebe evento subscription_authorized_payment
2. SubscriptionWebhookHandler.process() é chamado
3. Extrai subscription_id (preapproval_id) e payment_id
4. Busca assinatura pelo preapproval_id
5. Busca pagamento completo via API do Mercado Pago
6. Cria/atualiza Payment vinculado à assinatura
7. Se pagamento aprovado:
   - Atualiza status da assinatura para ACTIVE
   - Atualiza currentPeriodStart e currentPeriodEnd (+1 mês)
8. Salva assinatura e pagamento
```

**Status:** ✅ **FUNCIONANDO CORRETAMENTE**

---

## 🔧 CORREÇÕES REALIZADAS

### 1. **MercadoPagoService.processWebhookNotification()**

#### ❌ **ANTES:**
```java
// Tentava buscar Preapproval usando Preference (ERRADO)
Preference preference = getPreference(dataId);
```

#### ✅ **AGORA:**
```java
// Busca Preapproval via API REST correta
Map<String, Object> preapprovalData = getPreapproval(dataId);
```

**Mudança:** Adicionado método `getPreapproval()` que busca via `/preapproval/{id}`

### 2. **WebhookService.handleSubscriptionPreapproval()**

#### ❌ **ANTES:**
```java
String preferenceId = (String) preapprovalData.get("id");
log.info("Preference ID: {}", preferenceId);
```

#### ✅ **AGORA:**
```java
String preapprovalId = (String) preapprovalData.get("id");
log.info("Preapproval ID: {}", preapprovalId);
```

**Mudança:** Corrigido nomes de variáveis e logs para refletir que é Preapproval, não Preference

### 3. **WebhookService.createSubscriptionFromPreapproval()**

#### ❌ **ANTES:**
```java
private Subscription createSubscriptionFromPreapproval(
    Map<String, Object> preapprovalData, String preferenceId) {
    // ...
    subscription.setStripeSubscriptionId(preferenceId);
}
```

#### ✅ **AGORA:**
```java
private Subscription createSubscriptionFromPreapproval(
    Map<String, Object> preapprovalData, String preapprovalId) {
    // ...
    subscription.setStripeSubscriptionId(preapprovalId); // Preapproval ID
    log.info("✓ Nova assinatura criada: PreapprovalId={}", preapprovalId);
}
```

**Mudança:** Corrigido para usar `preapprovalId` e adicionado log claro

---

## 📌 PONTOS IMPORTANTES

### ✅ **Armazenamento do ID**

- O **preapproval_id** é armazenado no campo `stripeSubscriptionId` da tabela `subscriptions`
- Isso permite buscar a assinatura quando receber webhooks de renovação
- **NÃO confundir** com `preference_id` (que era usado antes)

### ✅ **Busca de Assinatura**

Quando o webhook `subscription_authorized_payment` chega:

1. O `subscription_id` no payload é o **preapproval_id**
2. O código busca a assinatura usando:
   ```java
   subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
   ```
3. Isso funciona porque o `preapproval_id` está salvo em `stripeSubscriptionId`

### ✅ **Processamento de Pagamentos Recorrentes**

O fluxo de renovação mensal está **correto**:

1. ✅ Mercado Pago cobra automaticamente todo mês
2. ✅ Envia webhook `subscription_authorized_payment`
3. ✅ Back-end busca pagamento via API
4. ✅ Cria/atualiza Payment no banco
5. ✅ Atualiza período da assinatura (+1 mês)
6. ✅ Mantém assinatura ativa

---

## 🔍 VERIFICAÇÕES ADICIONAIS

### ✅ **Eventos Suportados**

| Tipo de Evento | Descrição | Status |
|----------------|-----------|--------|
| `subscription_preapproval` | Criação/atualização de assinatura | ✅ Tratado |
| `subscription_authorized_payment` | Pagamento de renovação mensal | ✅ Tratado |
| `payment` | Pagamento único ou de renovação | ✅ Tratado |

### ✅ **Mapeamento de Status**

O código mapeia corretamente os status do Mercado Pago:

| Status Mercado Pago | Status Local | Ação |
|---------------------|--------------|------|
| `authorized` | `ACTIVE` | Ativa assinatura |
| `paused` | `PAUSED` | Pausa assinatura |
| `cancelled` | `CANCELED` | Cancela assinatura |
| `pending` | `PENDING` | Mantém pendente |

### ✅ **Idempotência**

- ✅ Verifica se Payment já existe antes de criar
- ✅ Verifica se Subscription já existe antes de criar
- ✅ Usa `webhook_events` para evitar processamento duplicado

---

## 📝 CHECKLIST FINAL

- [x] **Webhook `subscription_preapproval` processa corretamente**
- [x] **Webhook `subscription_authorized_payment` processa corretamente**
- [x] **Busca Preapproval via API REST correta**
- [x] **Armazena preapproval_id corretamente**
- [x] **Renovação mensal funciona**
- [x] **Pagamentos recorrentes são registrados**
- [x] **Status da assinatura é atualizado corretamente**
- [x] **Período de renovação é atualizado (+1 mês)**

---

## 🎯 CONCLUSÃO

**✅ TODOS OS WEBHOOKS ESTÃO CORRETOS E FUNCIONANDO**

Os webhooks foram verificados e corrigidos para trabalhar com **PreapprovalPlan**. O sistema agora:

1. ✅ Processa corretamente criação de assinaturas recorrentes
2. ✅ Processa corretamente renovações mensais
3. ✅ Registra pagamentos recorrentes
4. ✅ Atualiza status e períodos corretamente

**Nenhuma mudança adicional é necessária nos webhooks.**

---

## 📚 REFERÊNCIAS

- **Mercado Pago API:** `/preapproval_plan` e `/preapproval`
- **Webhook Events:** `subscription_preapproval`, `subscription_authorized_payment`
- **Documentação:** `docs/webhooks-mercadopago.md`

---

**Documento criado em:** 2025-01-15  
**Última atualização:** 2025-01-15







