# 📚 Runbooks Operacionais - Webhooks Mercado Pago

**Versão:** 1.0  
**Última Atualização:** 2025-01-15

---

## 🎯 OBJETIVO

Este documento contém procedimentos operacionais para gerenciar webhooks do Mercado Pago em produção.

---

## 1. 🔄 REPROCESSAMENTO DE EVENTOS FALHADOS

### 1.1 Listar Eventos Falhados

**Endpoint:** `GET /admin/webhooks/failed`

**Autenticação:** Requer role `ADMIN`

**Exemplo:**
```bash
curl -X GET "https://seu-dominio.com/admin/webhooks/failed" \
  -H "Authorization: Bearer <token_admin>"
```

**Resposta:**
```json
[
  {
    "id": 1,
    "eventId": "123456789",
    "eventType": "payment",
    "errorMessage": "Assinatura não encontrada",
    "retryCount": 2,
    "createdAt": "2025-01-15T10:30:00"
  }
]
```

### 1.2 Reprocessar Evento Específico

**Endpoint:** `POST /admin/webhooks/failed/{eventId}/reprocess`

**Autenticação:** Requer role `ADMIN`

**Exemplo:**
```bash
curl -X POST "https://seu-dominio.com/admin/webhooks/failed/123456789/reprocess" \
  -H "Authorization: Bearer <token_admin>"
```

**Resposta:**
```json
{
  "status": "success",
  "message": "Evento reprocessado com sucesso",
  "eventId": "123456789"
}
```

### 1.3 Reprocessar Lote de Eventos

**Endpoint:** `POST /admin/webhooks/failed/reprocess/batch`

**Autenticação:** Requer role `ADMIN`

**Body:**
```json
{
  "eventIds": ["123456789", "987654321"],
  "eventType": "payment"
}
```

**Exemplo:**
```bash
curl -X POST "https://seu-dominio.com/admin/webhooks/failed/reprocess/batch" \
  -H "Authorization: Bearer <token_admin>" \
  -H "Content-Type: application/json" \
  -d '{"eventType": "payment"}'
```

---

## 2. 🔍 INVESTIGAÇÃO DE ERROS

### 2.1 Verificar Logs de Webhook

**Formato de Log:**
```
[WEBHOOK] event=webhook.processing.error requestId=abc123 eventId=123456789 eventType=payment error=WebhookProcessingException errorMessage=Assinatura não encontrada timestamp=2025-01-15T10:30:00
```

**Comandos Úteis:**
```bash
# Filtrar logs de webhook
grep "\[WEBHOOK\]" application.log

# Filtrar erros
grep "webhook.processing.error" application.log

# Filtrar por eventId
grep "eventId=123456789" application.log
```

### 2.2 Verificar Evento Específico

**Endpoint:** `GET /admin/webhooks/failed/{eventId}`

**Exemplo:**
```bash
curl -X GET "https://seu-dominio.com/admin/webhooks/failed/123456789" \
  -H "Authorization: Bearer <token_admin>"
```

**Resposta:**
```json
{
  "id": 1,
  "eventId": "123456789",
  "eventType": "payment",
  "payload": "{...}",
  "errorMessage": "Assinatura não encontrada",
  "errorStackTrace": "...",
  "retryCount": 2,
  "lastRetryAt": "2025-01-15T11:00:00",
  "createdAt": "2025-01-15T10:30:00"
}
```

### 2.3 Verificar Métricas

**Endpoint:** `GET /actuator/metrics` (se Prometheus configurado)

**Ou consultar diretamente:**
```java
WebhookMonitoringService monitoringService = ...;
WebhookMetrics metrics = monitoringService.getMetrics();
```

**Métricas Disponíveis:**
- `totalReceived` - Total recebido
- `totalProcessed` - Total processado
- `totalFailed` - Total falhado
- `successRate` - Taxa de sucesso (%)
- `failureRate` - Taxa de falha (%)
- `unprocessedCount` - Eventos não processados

---

## 3. ⚙️ ATIVAÇÃO/DESATIVAÇÃO DE WEBHOOKS

### 3.1 Desabilitar Webhook Temporariamente

**Método 1: Via Painel do Mercado Pago**
1. Acessar: https://www.mercadopago.com.br/developers/panel/app
2. Navegar para configurações de Webhooks
3. Desabilitar eventos específicos ou todos

**Método 2: Via Código (NÃO RECOMENDADO)**
- Comentar endpoint no controller (requer deploy)
- Usar feature flag (se implementado)

### 3.2 Reativar Webhook

**Via Painel do Mercado Pago:**
1. Acessar painel
2. Reabilitar eventos desejados
3. Verificar que URL está correta

**Validação:**
```bash
# Verificar se endpoint está respondendo
curl -X POST "https://seu-dominio.com/webhooks/mercadopago" \
  -H "x-signature: ts=123,v1=test" \
  -d '{"type":"test","data":{"id":"test"}}'
```

---

## 4. 🚨 PROCEDIMENTOS DE EMERGÊNCIA

### 4.1 Muitos Eventos Falhados

**Sintomas:**
- Taxa de falha > 5%
- Muitos eventos em `failed_webhook_events`
- Alertas sendo disparados

**Ações:**
1. Verificar logs para identificar padrão de erro
2. Verificar conectividade com Mercado Pago API
3. Verificar se webhook secret está correto
4. Reprocessar eventos falhados em lote
5. Se necessário, desabilitar webhook temporariamente

### 4.2 Chargeback Detectado

**Sintomas:**
- Alerta imediato recebido
- Payment marcado como `CHARGED_BACK`
- Subscription suspensa

**Ações:**
1. Verificar detalhes do chargeback no painel do Mercado Pago
2. Verificar histórico em `chargeback_history`
3. Confirmar que subscription está suspensa
4. Verificar que acesso está bloqueado
5. Documentar para análise posterior

### 4.3 Webhooks Não Processados

**Sintomas:**
- Eventos com `processed = false` há mais de 5 minutos
- Job de reprocessamento não está funcionando

**Ações:**
1. Verificar se job está executando (logs)
2. Verificar conectividade com banco de dados
3. Reprocessar manualmente via endpoint admin
4. Verificar se há muitos eventos (pode indicar problema maior)

---

## 5. 📞 CONTATOS DE SUPORTE

### 5.1 Mercado Pago

**Suporte Técnico:**
- Email: developers@mercadopago.com
- Documentação: https://www.mercadopago.com.br/developers/pt/docs
- Painel: https://www.mercadopago.com.br/developers/panel/app

### 5.2 Equipe Interna

**Contatos de Emergência:**
- Email: devops@empresa.com
- Slack: #webhooks-alerts
- PagerDuty: (se configurado)

---

## 6. 🔧 MANUTENÇÃO PREVENTIVA

### 6.1 Limpeza de Eventos Antigos

**Eventos não processados > 30 dias:**
- São automaticamente ignorados pelo job de reprocessamento
- Podem ser removidos manualmente se necessário

**Query SQL:**
```sql
-- Listar eventos não processados há mais de 30 dias
SELECT * FROM webhook_events 
WHERE processed = false 
AND created_at < NOW() - INTERVAL '30 days';
```

### 6.2 Monitoramento de Saúde

**Verificar diariamente:**
- Taxa de sucesso/falha
- Eventos não processados
- Tempo médio de processamento
- Alertas ativos

**Endpoint de Health:**
```bash
curl https://seu-dominio.com/actuator/health
```

---

## 7. 📊 DASHBOARDS E RELATÓRIOS

### 7.1 Consultas Úteis

**Eventos por tipo (últimas 24h):**
```sql
SELECT event_type, COUNT(*) 
FROM webhook_events 
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY event_type;
```

**Taxa de sucesso:**
```sql
SELECT 
  COUNT(*) FILTER (WHERE processed = true) * 100.0 / COUNT(*) as success_rate
FROM webhook_events 
WHERE created_at > NOW() - INTERVAL '24 hours';
```

**Eventos falhados por tipo:**
```sql
SELECT event_type, COUNT(*) 
FROM failed_webhook_events 
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY event_type;
```

---

**Última Atualização:** 2025-01-15  
**Versão:** 1.0







