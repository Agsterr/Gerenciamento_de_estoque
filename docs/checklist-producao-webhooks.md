# ✅ Checklist Final para Produção - Webhooks Mercado Pago

Este documento contém o checklist completo para garantir que o sistema de webhooks esteja pronto para produção.

---

## 🔒 1. SEGURANÇA

### 1.1 Validação de Assinatura
- [ ] **Webhook Secret configurado** no `application.properties` ou variáveis de ambiente
  - Variável: `MERCADOPAGO_WEBHOOK_SECRET`
  - ✅ Verificar que o secret está configurado no painel do Mercado Pago
  - ✅ Secret NÃO deve estar hardcoded no código
  - ✅ Secret deve ser diferente entre ambiente de teste e produção

- [ ] **Validação de assinatura implementada**
  - ✅ `WebhookSignatureValidator` validando header `x-signature`
  - ✅ Formato correto: `ts=<timestamp>,v1=<hash>`
  - ✅ Algoritmo HMAC-SHA256 implementado corretamente
  - ✅ Comparação timing-safe para evitar timing attacks
  - ✅ Rejeição com HTTP 401 para assinaturas inválidas

- [ ] **Endpoint público protegido**
  - ✅ Endpoint `/webhooks/mercadopago` é público (necessário para receber webhooks)
  - ✅ Validação de assinatura ANTES de processar qualquer lógica
  - ✅ Rate limiting configurado (opcional, mas recomendado)

### 1.2 Dados Sensíveis
- [ ] **Logs não expõem dados sensíveis**
  - ✅ Payloads completos apenas em nível DEBUG
  - ✅ Secrets nunca logados
  - ✅ Dados de cartão não logados
  - ✅ CPF/CNPJ mascarados se necessário

- [ ] **HTTPS obrigatório**
  - ✅ Aplicação rodando apenas em HTTPS em produção
  - ✅ Certificado SSL válido
  - ✅ Redirecionamento HTTP → HTTPS configurado

### 1.3 Acesso e Permissões
- [ ] **Endpoints admin protegidos**
  - ✅ `/admin/webhooks/failed/**` requer autenticação ADMIN
  - ✅ Spring Security configurado corretamente
  - ✅ Roles e permissões testadas

---

## 📝 2. LOGS

### 2.1 Logs Estruturados
- [ ] **Formato de logs padronizado**
  - ✅ Logs estruturados com formato: `[WEBHOOK] event=... key=value ...`
  - ✅ Eventos principais logados:
    - `webhook.received` - Recebimento do webhook
    - `webhook.processing.started` - Início do processamento
    - `webhook.processing.success` - Sucesso
    - `webhook.processing.error` - Erro
    - `webhook.event.saved` - Evento salvo
    - `webhook.event.duplicate` - Evento duplicado
    - `webhook.validation.failed` - Validação falhou

- [ ] **Informações essenciais em cada log**
  - ✅ `requestId` - ID único da requisição
  - ✅ `eventId` - ID do evento do webhook
  - ✅ `eventType` - Tipo do evento (payment, chargeback, etc.)
  - ✅ `timestamp` - Data/hora do evento
  - ✅ `durationMs` - Tempo de processamento (quando aplicável)
  - ✅ `error` - Tipo de erro (quando aplicável)

### 2.2 Níveis de Log
- [ ] **Configuração de níveis**
  - ✅ INFO: Eventos principais e sucessos
  - ✅ WARN: Eventos duplicados, validações falhadas
  - ✅ ERROR: Erros de processamento, falhas críticas
  - ✅ DEBUG: Payloads completos, detalhes técnicos

- [ ] **Logs de auditoria**
  - ✅ Todos os webhooks recebidos são logados
  - ✅ Histórico de chargebacks registrado
  - ✅ Alterações de status de pagamento logadas
  - ✅ Alterações de assinatura logadas

### 2.3 Retenção de Logs
- [ ] **Política de retenção**
  - ✅ Logs mantidos por período mínimo (ex: 30 dias)
  - ✅ Logs críticos mantidos por período maior (ex: 1 ano)
  - ✅ Sistema de rotação de logs configurado

---

## 📊 3. MONITORAMENTO

### 3.1 Métricas Essenciais
- [ ] **Métricas de recebimento**
  - ✅ Total de webhooks recebidos (por tipo)
  - ✅ Taxa de webhooks válidos vs inválidos
  - ✅ Tempo médio de processamento
  - ✅ Taxa de sucesso vs falha

- [ ] **Métricas de processamento**
  - ✅ Eventos processados com sucesso
  - ✅ Eventos duplicados ignorados
  - ✅ Eventos falhados (salvos em `failed_webhook_events`)
  - ✅ Tempo de processamento por tipo de evento

- [ ] **Métricas de negócio**
  - ✅ Pagamentos processados
  - ✅ Chargebacks detectados
  - ✅ Assinaturas ativadas/suspensas
  - ✅ Atualizações de cartão

### 3.2 Alertas
- [ ] **Alertas críticos configurados**
  - ✅ Taxa de falha > 5% (últimas 100 requisições)
  - ✅ Webhooks não processados por > 5 minutos
  - ✅ Muitos eventos falhados (> 10 em 1 hora)
  - ✅ Validação de assinatura falhando frequentemente
  - ✅ Timeout de processamento

- [ ] **Alertas de negócio**
  - ✅ Chargeback detectado (alerta imediato)
  - ✅ Pagamento aprovado não processado
  - ✅ Assinatura não ativada após pagamento aprovado

### 3.3 Dashboards
- [ ] **Dashboard de webhooks**
  - ✅ Gráfico de webhooks recebidos por hora/dia
  - ✅ Taxa de sucesso por tipo de evento
  - ✅ Top eventos falhados
  - ✅ Tempo médio de processamento

- [ ] **Dashboard de negócio**
  - ✅ Pagamentos aprovados vs rejeitados
  - ✅ Chargebacks por período
  - ✅ Assinaturas ativas/suspensas

### 3.4 Health Checks
- [ ] **Endpoints de health**
  - ✅ `/actuator/health` funcionando
  - ✅ Health check específico para webhooks (opcional)
  - ✅ Verificação de conectividade com banco de dados
  - ✅ Verificação de conectividade com Mercado Pago API

---

## 🔄 4. RETENTATIVAS

### 4.1 Fila de Eventos Falhados
- [ ] **Tabela `failed_webhook_events`**
  - ✅ Tabela criada e migrada
  - ✅ Índices criados para performance
  - ✅ Campos essenciais: `event_id`, `event_type`, `payload`, `error_message`, `retry_count`

- [ ] **Salvamento de eventos falhados**
  - ✅ Todos os erros são salvos automaticamente
  - ✅ Payload completo salvo para reprocessamento
  - ✅ Stack trace salvo para debugging
  - ✅ Contador de retentativas incrementado

### 4.2 Reprocessamento Manual
- [ ] **Endpoint admin para reprocessamento**
  - ✅ `POST /admin/webhooks/failed/{eventId}/reprocess` funcionando
  - ✅ `POST /admin/webhooks/failed/reprocess/batch` para lote
  - ✅ Autenticação ADMIN obrigatória
  - ✅ Logs de reprocessamento

- [ ] **Interface de visualização**
  - ✅ `GET /admin/webhooks/failed` - Lista todos os eventos falhados
  - ✅ `GET /admin/webhooks/failed/{eventId}` - Detalhes de um evento
  - ✅ `GET /admin/webhooks/failed/type/{eventType}` - Filtrar por tipo
  - ✅ Ordenação por data (mais recentes primeiro)

### 4.3 Retentativas Automáticas (Opcional)
- [ ] **Job de retentativa automática**
  - ⚠️ Implementar job agendado para reprocessar eventos falhados
  - ⚠️ Limite de retentativas (ex: máximo 3 tentativas)
  - ⚠️ Backoff exponencial entre retentativas
  - ⚠️ Excluir eventos muito antigos (ex: > 30 dias)

---

## 📚 5. CONFORMIDADE COM DOCUMENTAÇÃO DO MERCADO PAGO

### 5.1 Formato de Webhook
- [ ] **Estrutura do payload**
  - ✅ Recebe JSON no formato: `{ "id": "...", "type": "...", "data": { "id": "..." } }`
  - ✅ Campo `id` do webhook identificado corretamente
  - ✅ Campo `type` identificado corretamente
  - ✅ Campo `data.id` usado como `eventId` para idempotência

- [ ] **Headers obrigatórios**
  - ✅ Header `x-signature` validado
  - ✅ Header `x-request-id` capturado (opcional, mas recomendado)
  - ✅ Content-Type: `application/json`

### 5.2 Resposta HTTP
- [ ] **Códigos de status corretos**
  - ✅ HTTP 200: Webhook recebido e aceito (sempre retornar, mesmo em erro)
  - ✅ HTTP 401: Assinatura inválida (rejeitar imediatamente)
  - ✅ HTTP 400: Payload inválido (campos obrigatórios ausentes)

- [ ] **Tempo de resposta**
  - ✅ Resposta HTTP 200 retornada em < 2 segundos
  - ✅ Processamento pesado feito de forma assíncrona
  - ✅ Timeout configurado (padrão: 30 segundos)

### 5.3 Tipos de Eventos Suportados
- [ ] **Eventos implementados**
  - ✅ `payment` - Processamento de pagamentos
  - ✅ `chargebacks` - Chargebacks e reclamações
  - ✅ `merchant_order` - Pedidos comerciais
  - ✅ `subscriptions` - Assinaturas (preapproval, authorized_payment)
  - ✅ `card.updated` - Atualização de cartão

- [ ] **Handlers específicos**
  - ✅ `PaymentWebhookHandler` implementado
  - ✅ `ChargebackWebhookHandler` implementado
  - ✅ `MerchantOrderWebhookHandler` implementado
  - ✅ `SubscriptionWebhookHandler` implementado
  - ✅ `CardWebhookHandler` implementado

### 5.4 Validação de Assinatura
- [ ] **Algoritmo conforme documentação**
  - ✅ Parse do header: `ts=<timestamp>,v1=<hash>`
  - ✅ Construção: `signedPayload = ts + "." + requestBody`
  - ✅ Hash: `HMAC-SHA256(secret, signedPayload)`
  - ✅ Comparação timing-safe

- [ ] **Secret do webhook**
  - ✅ Secret obtido do painel de Webhooks do Mercado Pago
  - ✅ Secret configurado via variável de ambiente
  - ✅ Secret diferente para teste e produção

### 5.5 Idempotência
- [ ] **Garantia de idempotência**
  - ✅ Tabela `webhook_events` criada
  - ✅ Campo `event_id` único (constraint UNIQUE)
  - ✅ Verificação antes de processar: `existsByEventId()`
  - ✅ Salvamento ANTES do processamento
  - ✅ Tratamento de race conditions (DataIntegrityViolationException)

### 5.6 Processamento de Pagamentos
- [ ] **Fluxo conforme documentação**
  - ✅ Busca detalhes completos via API: `GET /v1/payments/{id}`
  - ✅ NÃO confia apenas no payload do webhook
  - ✅ Mapeamento de status correto:
    - `approved` → `APPROVED`
    - `pending` → `PENDING`
    - `rejected` → `REJECTED`
    - `cancelled` → `CANCELLED`
    - `refunded` → `REFUNDED`
    - `charged_back` → `CHARGED_BACK`

- [ ] **Regras de negócio**
  - ✅ Assinatura NÃO ativada se status != `APPROVED`
  - ✅ Produto NÃO liberado se status != `APPROVED`
  - ✅ Link Payment ↔ Subscription garantido

### 5.7 Chargebacks
- [ ] **Processamento de chargebacks**
  - ✅ Identificação de criação ou mudança de status
  - ✅ Pagamento marcado como em disputa (`in_dispute = true`)
  - ✅ Assinatura suspensa e acesso bloqueado
  - ✅ Histórico registrado em `chargeback_history`
  - ✅ Reativação automática NÃO permitida

### 5.8 Merchant Orders
- [ ] **Processamento de pedidos**
  - ✅ Busca ordem via API: `GET /v1/merchant_orders/{id}`
  - ✅ Soma de pagamentos aprovados
  - ✅ Comparação com total da ordem
  - ✅ Status: PAGO, PARCIAL, NÃO PAGO
  - ✅ Liberação apenas quando totalmente pago

### 5.9 Subscriptions
- [ ] **Processamento de assinaturas**
  - ✅ Evento `subscription_preapproval` tratado
  - ✅ Evento `subscription_authorized_payment` tratado
  - ✅ Status de assinatura atualizado corretamente
  - ✅ Renovação mensal garantida

### 5.10 Card Updates
- [ ] **Atualização de cartão**
  - ✅ Evento `card.updated` tratado
  - ✅ `card_id` atualizado na assinatura
  - ✅ Histórico salvo em `card_history`
  - ✅ Logs de auditoria

---

## 🧪 6. TESTES

### 6.1 Testes Unitários
- [ ] **Cobertura de testes**
  - ✅ Testes de validação de assinatura
  - ✅ Testes de idempotência
  - ✅ Testes de processamento de eventos
  - ✅ Testes simulando payloads reais

- [ ] **Testes executando**
  - ✅ Todos os testes passando
  - ✅ Cobertura de código > 80% (recomendado)
  - ✅ Testes executados no CI/CD

### 6.2 Testes de Integração
- [ ] **Testes end-to-end**
  - ✅ Teste completo de recebimento de webhook
  - ✅ Teste de validação de assinatura
  - ✅ Teste de processamento de pagamento
  - ✅ Teste de chargeback

### 6.3 Testes em Ambiente de Teste
- [ ] **Testes com Mercado Pago Sandbox**
  - ✅ Webhook configurado no painel de teste
  - ✅ Webhooks de teste recebidos e processados
  - ✅ Todos os tipos de eventos testados
  - ✅ Validação de assinatura funcionando

---

## ⚙️ 7. CONFIGURAÇÃO

### 7.1 Variáveis de Ambiente
- [ ] **Configurações obrigatórias**
  - ✅ `MERCADOPAGO_WEBHOOK_SECRET` - Secret do webhook
  - ✅ `MERCADOPAGO_ENVIRONMENT` - Ambiente (test/production)
  - ✅ `MERCADOPAGO_PROD_ACCESS_TOKEN` - Token de produção
  - ✅ `JDBC_URL` - URL do banco de dados
  - ✅ `DB_USER` - Usuário do banco
  - ✅ `DB_PASSWORD` - Senha do banco

- [ ] **Configurações opcionais**
  - ✅ `webhook.processing.timeout.seconds` - Timeout (padrão: 30)
  - ✅ `webhook.executor.core-pool-size` - Pool de threads (padrão: 5)
  - ✅ `webhook.executor.max-pool-size` - Pool máximo (padrão: 10)
  - ✅ `webhook.executor.queue-capacity` - Capacidade da fila (padrão: 100)

### 7.2 Banco de Dados
- [ ] **Migrations executadas**
  - ✅ `V13__add_mercado_pago_payment_id.sql`
  - ✅ `V14__Create_webhook_events_table.sql`
  - ✅ `V15__Create_failed_webhook_events_table.sql`
  - ✅ `V16__Add_card_support.sql`
  - ✅ `V17__Add_chargeback_history.sql`

- [ ] **Índices criados**
  - ✅ Índices em `webhook_events.event_id`
  - ✅ Índices em `payments.mercado_pago_payment_id`
  - ✅ Índices em `failed_webhook_events.event_id`
  - ✅ Índices em `chargeback_history.chargeback_id`

### 7.3 Painel do Mercado Pago
- [ ] **Configuração no painel**
  - ✅ URL do webhook configurada: `https://seu-dominio.com/webhooks/mercadopago`
  - ✅ Secret do webhook copiado e configurado
  - ✅ Eventos habilitados:
    - ✅ `payment`
    - ✅ `chargebacks`
    - ✅ `merchant_order`
    - ✅ `subscriptions`
    - ✅ `card.updated`
  - ✅ Ambiente de produção configurado (não sandbox)

---

## 🚀 8. DEPLOY

### 8.1 Pré-Deploy
- [ ] **Checklist pré-deploy**
  - ✅ Código revisado e aprovado
  - ✅ Testes passando
  - ✅ Migrations testadas
  - ✅ Variáveis de ambiente configuradas
  - ✅ Secret do webhook obtido do painel

### 8.2 Deploy
- [ ] **Processo de deploy**
  - ✅ Deploy realizado em horário de baixo tráfego (se possível)
  - ✅ Migrations executadas com sucesso
  - ✅ Aplicação iniciada sem erros
  - ✅ Health checks passando
  - ✅ Logs sendo gerados corretamente

### 8.3 Pós-Deploy
- [ ] **Validação pós-deploy**
  - ✅ Webhook recebido e processado com sucesso
  - ✅ Validação de assinatura funcionando
  - ✅ Eventos sendo salvos no banco
  - ✅ Logs estruturados aparecendo
  - ✅ Métricas sendo coletadas

---

## 📋 9. DOCUMENTAÇÃO

### 9.1 Documentação Técnica
- [ ] **Documentação interna**
  - ✅ `docs/webhooks-mercadopago.md` - Documentação completa
  - ✅ `docs/checklist-producao-webhooks.md` - Este checklist
  - ✅ Comentários no código explicando lógica complexa

### 9.2 Documentação Operacional
- [ ] **Runbooks**
  - ✅ Procedimento de reprocessamento de eventos falhados
  - ✅ Procedimento de investigação de erros
  - ✅ Procedimento de ativação/desativação de webhooks
  - ✅ Contatos de suporte

---

## 🔍 10. VALIDAÇÃO FINAL

### 10.1 Teste End-to-End
- [ ] **Fluxo completo testado**
  - ✅ Webhook recebido do Mercado Pago
  - ✅ Assinatura validada
  - ✅ Evento processado
  - ✅ Dados persistidos no banco
  - ✅ Assinatura ativada (se pagamento aprovado)
  - ✅ Logs gerados corretamente

### 10.2 Validação de Negócio
- [ ] **Cenários críticos testados**
  - ✅ Pagamento aprovado → Assinatura ativada
  - ✅ Chargeback → Assinatura suspensa
  - ✅ Evento duplicado → Ignorado (idempotência)
  - ✅ Assinatura inválida → Rejeitada (HTTP 401)
  - ✅ Timeout → Evento salvo como falhado

### 10.3 Performance
- [ ] **Performance validada**
  - ✅ Tempo de resposta < 2 segundos
  - ✅ Processamento assíncrono funcionando
  - ✅ Pool de threads configurado adequadamente
  - ✅ Sem memory leaks
  - ✅ Banco de dados com performance adequada

---

## 📞 11. SUPORTE E CONTINGÊNCIA

### 11.1 Plano de Contingência
- [ ] **Procedimentos de emergência**
  - ✅ Procedimento para desabilitar webhooks temporariamente
  - ✅ Procedimento para reprocessar eventos em lote
  - ✅ Procedimento para investigar eventos perdidos
  - ✅ Contatos de emergência do Mercado Pago

### 11.2 Monitoramento Ativo
- [ ] **Vigilância inicial**
  - ✅ Monitoramento ativo nas primeiras 24 horas
  - ✅ Verificação de logs a cada hora
  - ✅ Verificação de eventos falhados
  - ✅ Verificação de métricas

---

## ✅ CONCLUSÃO

Após completar todos os itens deste checklist, o sistema de webhooks estará pronto para produção.

**Última atualização:** 2025-01-15  
**Versão:** 1.0

---

## 📝 NOTAS

- ⚠️ = Opcional mas recomendado
- ✅ = Obrigatório
- 🔴 = Crítico - não pode ir para produção sem isso

---

## 🔗 LINKS ÚTEIS

- [Documentação Oficial Mercado Pago Webhooks](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks)
- [Documentação Interna](./webhooks-mercadopago.md)
- [Painel de Webhooks Mercado Pago](https://www.mercadopago.com.br/developers/panel/app)







