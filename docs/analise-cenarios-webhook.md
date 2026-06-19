# Análise de Cenários de Webhook - Sistema de Preparação

Este documento analisa 5 cenários críticos de webhook e verifica se o sistema está preparado para lidar com eles.

---

## 1. Webhook Duplicado (mesmo event_id)

### O que acontece hoje:

✅ **SISTEMA ESTÁ PREPARADO**

O sistema possui mecanismos robustos de idempotência:

1. **Verificação dupla de idempotência:**
   - Linha 675: `webhookEventRepository.existsByEventId(eventId)` - verifica antes de processar
   - Linha 519: `ensureEventIdSaved()` - salva event_id ANTES de processar
   - Linha 688-707: Tenta salvar e trata `DataIntegrityViolationException` em caso de race condition

2. **Tratamento de race conditions:**
   - Linha 534-536: Captura `DataIntegrityViolationException` quando outro thread já salvou
   - Linha 697-707: Tratamento de duplicação durante o save

3. **Constraint de unicidade no banco:**
   - `WebhookEvent` possui constraint UNIQUE em `event_id` (linha 16 do modelo)

4. **Resposta adequada:**
   - Linha 682: Retorna `WebhookResponseDto.alreadyProcessed(eventId, type)`
   - Log estruturado de duplicação (linha 676-681, 700-705)

### O que deveria acontecer:

✅ **Já está implementado corretamente**

- Evento duplicado é detectado e ignorado
- Retorna resposta HTTP 200 com status "already_processed"
- Log estruturado para auditoria
- Não processa novamente o mesmo evento

### Ajustes necessários:

✅ **Nenhum ajuste necessário** - Sistema está robusto

---

## 2. Webhook Fora de Ordem (payment aprovado antes do pending)

### O que acontece hoje:

⚠️ **SISTEMA PARCIALMENTE PREPARADO - REQUER MELHORIAS**

**Problemas identificados:**

1. **Não há verificação de ordem temporal:**
   - Linha 907-919: Verifica se payment já existe, mas apenas ignora se já está APPROVED
   - Não verifica se um status mais antigo está sendo processado após um mais recente

2. **Atualização sem validação de ordem:**
   - Linha 982-993: `createOrUpdatePayment()` sempre atualiza o status, mesmo que seja um status mais antigo
   - Não há verificação de timestamp do evento

3. **Busca status atualizado via API:**
   - Linha 898: `mercadoPagoService.getPayment(paymentId)` - busca status atualizado
   - ✅ Isso ajuda, mas não resolve completamente o problema de ordem

**Comportamento atual:**
- Se receber "approved" antes de "pending", o sistema:
  1. Busca payment via API (linha 898) - obtém status atualizado
  2. Se payment já existe e está APPROVED, ignora (linha 914-918)
  3. Se não existe, cria com status APPROVED
  4. Quando "pending" chegar depois, atualiza para PENDING (linha 982-993)

**Problema:** Status pode ser regredido de APPROVED para PENDING se webhook "pending" chegar depois.

### O que deveria acontecer:

1. **Validar ordem temporal dos eventos:**
   - Comparar timestamp do evento com timestamp do último status processado
   - Ignorar eventos mais antigos que o status atual

2. **Manter histórico de transições:**
   - Registrar todas as mudanças de status com timestamp
   - Permitir auditoria de ordem de eventos

3. **Regra de negócio:**
   - Se payment está APPROVED e recebe PENDING mais antigo → ignorar
   - Se payment está PENDING e recebe APPROVED → atualizar
   - Sempre usar status mais recente baseado em timestamp

### Ajustes necessários:

```java
// Adicionar ao Payment model:
@Column(name = "last_status_update_at")
private LocalDateTime lastStatusUpdateAt;

// Modificar handleMercadoPagoPayment:
// 1. Verificar timestamp do evento (se disponível no webhook)
// 2. Comparar com lastStatusUpdateAt do payment existente
// 3. Ignorar se evento é mais antigo que último status processado

// Exemplo de lógica:
if (existingPaymentOpt.isPresent()) {
    Payment existing = existingPaymentOpt.get();
    LocalDateTime eventTimestamp = extractEventTimestamp(mpPayment); // do webhook ou API
    if (existing.getLastStatusUpdateAt() != null && 
        eventTimestamp.isBefore(existing.getLastStatusUpdateAt())) {
        log.warn("Evento mais antigo ignorado - payment {} já tem status mais recente", paymentId);
        return; // Ignorar evento fora de ordem
    }
}
```

**Prioridade:** MÉDIA - Pode causar inconsistências, mas API sempre retorna status atualizado

---

## 3. Chargeback Após Assinatura Ativa

### O que acontece hoje:

✅ **SISTEMA ESTÁ PREPARADO**

**Implementação robusta:**

1. **Handler específico para chargeback:**
   - `ChargebackWebhookHandler` (linha 1-149 do handler)
   - Processa eventos de chargeback separadamente

2. **Ações implementadas:**
   - Linha 105-110: Marca payment como `CHARGED_BACK` e `inDispute = true`
   - Linha 113-125: Suspende subscription para `PAST_DUE` e bloqueia acesso (`accessBlocked = true`)
   - Linha 128-141: Registra histórico em `ChargebackHistory`
   - Linha 143-145: Bloqueia reativação automática

3. **Tratamento no WebhookService:**
   - Linha 1496-1504: Detecta status "charged_back" e chama `handlePaymentChargedBack()`
   - Linha 1520-1569: Processa chargeback com todas as ações necessárias

4. **Bloqueio de reativação:**
   - Linha 117: `subscription.setAccessBlocked(true)` - bloqueia acesso
   - Linha 1560-1563: Log explícito de que reativação automática está desabilitada

5. **Idempotência:**
   - Linha 98-102: Verifica se chargeback já foi processado antes de processar novamente

### O que deveria acontecer:

✅ **Já está implementado corretamente**

- Payment marcado como CHARGED_BACK
- Subscription suspensa (PAST_DUE)
- Acesso bloqueado (accessBlocked = true)
- Histórico registrado
- Reativação automática bloqueada
- Idempotência garantida

### Ajustes necessários:

✅ **Nenhum ajuste necessário** - Sistema está completo

**Observação:** O sistema também trata chargeback via `processPaymentStatusActions()` (linha 1496), garantindo que mesmo que venha como status de payment, será processado corretamente.

---

## 4. Webhook Recebido Durante Deploy/Restart

### O que acontece hoje:

⚠️ **SISTEMA PARCIALMENTE PREPARADO - REQUER MELHORIAS**

**Mecanismos existentes:**

1. **Processamento assíncrono:**
   - Linha 404: `@Async("webhookTaskExecutor")` - não bloqueia resposta HTTP
   - Linha 116-120: Controller retorna HTTP 200 imediatamente
   - ✅ Webhook não é perdido se servidor reiniciar após resposta HTTP 200

2. **Idempotência persistente:**
   - Linha 511-541: `ensureEventIdSaved()` salva event_id ANTES de processar
   - Linha 519: Verifica se já existe antes de salvar
   - ✅ Eventos são salvos no banco, sobrevivem a restarts

3. **Configuração de shutdown:**
   - Linha 47-48: `setWaitForTasksToCompleteOnShutdown(true)` e `setAwaitTerminationSeconds(60)`
   - ✅ Aguarda até 60 segundos para tarefas completarem durante shutdown

4. **Timeout controlado:**
   - Linha 481: Timeout configurável (padrão 30 segundos)
   - ✅ Evita processamento infinito

**Problemas identificados:**

1. **Perda de webhooks em processamento:**
   - Se webhook está sendo processado e servidor reinicia, pode ser perdido
   - `ensureEventIdSaved()` salva event_id, mas se processamento falhar após salvar, evento fica marcado como processado mas não foi realmente processado

2. **Fila de processamento não persistente:**
   - `ThreadPoolTaskExecutor` é em memória
   - Se servidor reiniciar, webhooks na fila são perdidos

3. **Falta de mecanismo de retry:**
   - Se processamento falhar após salvar event_id, não há retry automático
   - Depende de `FailedWebhookEventService` para reprocessamento manual

### O que deveria acontecer:

1. **Salvar event_id ANTES de processar (✅ já faz):**
   - Garantir que mesmo se processamento falhar, evento não será reprocessado

2. **Fila persistente ou mecanismo de retry:**
   - Usar fila persistente (ex: RabbitMQ, Redis) ou
   - Implementar retry automático para eventos falhados

3. **Graceful shutdown:**
   - ✅ Já implementado (aguarda 60 segundos)

4. **Verificação de eventos não processados:**
   - Job periódico para verificar eventos salvos mas não processados
   - Reprocessar eventos que falharam

### Ajustes necessários:

**Prioridade ALTA:**

1. **Adicionar flag de processamento completo:**
```java
// Modificar WebhookEvent model:
@Column(name = "processed", nullable = false)
private Boolean processed = false;

@Column(name = "processed_at")
private LocalDateTime processedAt;

// Modificar handleMercadoPagoWebhook:
// 1. Salvar event_id com processed = false
// 2. Após processamento bem-sucedido, marcar processed = true
// 3. Job periódico reprocessa eventos com processed = false
```

2. **Implementar job de reprocessamento:**
```java
@Scheduled(fixedDelay = 300000) // A cada 5 minutos
public void reprocessFailedWebhooks() {
    List<WebhookEvent> unprocessed = webhookEventRepository
        .findByProcessedFalseAndCreatedAtBefore(
            LocalDateTime.now().minusMinutes(5)
        );
    // Reprocessar eventos não processados
}
```

3. **Melhorar tratamento de falhas:**
   - Se processamento falhar após salvar event_id, marcar como failed mas não como processed
   - Permitir retry automático

**Prioridade MÉDIA:**

4. **Considerar fila persistente:**
   - Para ambientes críticos, usar RabbitMQ ou similar
   - Garantir que webhooks não sejam perdidos mesmo durante restart

---

## 5. Metadata Ausente no Pagamento

### O que acontece hoje:

✅ **SISTEMA ESTÁ PREPARADO COM FALLBACKS**

**Mecanismos de busca implementados:**

1. **Busca em múltiplas camadas (linha 1033-1139):**
   - **Prioridade 1:** `metadata.subscription_id` (linha 1053)
   - **Prioridade 2:** `metadata.preference_id` (linha 1054, 1078-1086)
   - **Prioridade 3:** `metadata.user_id` (linha 1055, 1089-1100)
   - **Prioridade 4:** Busca via API Mercado Pago (linha 1103-1124)
   - **Prioridade 5:** Busca via payment existente (linha 1127-1131)

2. **Tratamento de metadata ausente:**
   - Linha 1040-1045: Tenta obter metadata, trata exceção se não disponível
   - Linha 1051: Verifica se metadata não é null antes de usar
   - Linha 1102-1124: Se metadata não tiver dados, tenta buscar via API

3. **Validação obrigatória:**
   - Linha 968-979: Se subscription não for encontrada, lança exceção
   - ✅ Não cria payment sem subscription (constraint NOT NULL)

4. **Logs detalhados:**
   - Linha 1134-1136: Log de todas as tentativas de busca
   - Facilita debug quando metadata está ausente

**Comportamento atual:**
- Se metadata estiver ausente:
  1. Tenta buscar via metadata (falha silenciosamente)
  2. Tenta buscar via API Mercado Pago (pode não ter dados)
  3. Tenta buscar via payment existente (última tentativa)
  4. Se não encontrar, lança exceção e salva em `FailedWebhookEvent`

### O que deveria acontecer:

✅ **Já está implementado corretamente**

- Múltiplas estratégias de busca
- Fallbacks em caso de metadata ausente
- Validação obrigatória (não cria payment sem subscription)
- Logs detalhados para debug
- Salva evento falhado para reprocessamento manual

### Ajustes necessários:

✅ **Nenhum ajuste crítico necessário**

**Melhorias opcionais (prioridade BAIXA):**

1. **Melhorar busca via API:**
   - Linha 1118-1121: Comentário indica que busca de merchant_order não está implementada
   - Pode adicionar busca de merchant_order se necessário

2. **Adicionar métricas:**
   - Rastrear quantos pagamentos têm metadata ausente
   - Alertar se taxa de metadata ausente for alta

3. **Documentação:**
   - Documentar ordem de prioridade de busca
   - Adicionar exemplos de quando cada estratégia é usada

---

## Resumo Executivo

| Cenário | Status | Prioridade | Ação Necessária |
|---------|--------|------------|-----------------|
| 1. Webhook Duplicado | ✅ Preparado | - | Nenhuma |
| 2. Webhook Fora de Ordem | ⚠️ Parcial | MÉDIA | Adicionar validação de timestamp |
| 3. Chargeback Após Ativo | ✅ Preparado | - | Nenhuma |
| 4. Webhook Durante Deploy | ⚠️ Parcial | ALTA | Adicionar flag de processamento e job de retry |
| 5. Metadata Ausente | ✅ Preparado | - | Melhorias opcionais |

### Recomendações Prioritárias:

1. **ALTA:** Implementar flag `processed` em `WebhookEvent` e job de reprocessamento
2. **MÉDIA:** Adicionar validação de ordem temporal para evitar regressão de status
3. **BAIXA:** Melhorar documentação e métricas de metadata ausente

---

## Conclusão

O sistema está **bem preparado** para a maioria dos cenários críticos, com mecanismos robustos de idempotência, tratamento de chargeback e busca de assinatura. As principais melhorias necessárias são:

1. **Robustez durante deploy/restart** - Adicionar mecanismo de retry para eventos não processados
2. **Validação de ordem temporal** - Prevenir regressão de status quando webhooks chegam fora de ordem

Os outros cenários (duplicação, chargeback, metadata ausente) estão bem tratados pelo sistema atual.







