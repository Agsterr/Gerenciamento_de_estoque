# Integração Backend Stripe - Sistema de Gerenciamento de Estoque

## 📋 Visão Geral

Este documento descreve a implementação completa da integração backend com o Stripe para gerenciamento de assinaturas e pagamentos.

## 🏗️ Arquitetura Implementada

### Componentes Principais

1. **SubscriptionService** - Lógica de negócio para assinaturas
2. **SubscriptionController** - Endpoints REST para gerenciamento
3. **WebhookController** - Processamento de eventos Stripe
4. **StripeConfig** - Configuração e inicialização
5. **SecurityConfig** - Configuração de segurança atualizada
6. **RateLimitingInterceptor** - Proteção contra abuso

### Modelos de Dados

- **Subscription** - Entidade principal de assinatura
- **SubscriptionStatus** - Enum com status possíveis
- **SubscriptionDto** - DTO para transferência de dados

## 🔗 Endpoints Implementados

### Assinaturas (`/api/subscriptions`)

| Método | Endpoint | Descrição | Autenticação |
|--------|----------|-----------|-------------|
| GET | `/current` | Busca assinatura atual do usuário | JWT |
| POST | `/create` | Cria nova assinatura com checkout Stripe | JWT |
| POST | `/cancel` | Cancela assinatura ativa | JWT |
| GET | `/portal` | Gera link para portal do cliente Stripe | JWT |
| GET | `/history` | Lista histórico de assinaturas | JWT |
| GET | `/access/{feature}` | Verifica acesso a funcionalidade | JWT |
| GET | `/limits/{limitType}` | Verifica limites de uso | JWT |

### Webhooks (`/api/webhooks`)

| Método | Endpoint | Descrição | Autenticação |
|--------|----------|-----------|-------------|
| POST | `/stripe` | Processa eventos do Stripe | Signature |
| GET | `/stripe/test` | Teste de conectividade | Pública |

## 🔐 Segurança Implementada

### Autenticação JWT
- Todos os endpoints de assinatura requerem token JWT válido
- Extração automática de `userId` do token para operações

### Validação de Webhook
- Verificação de assinatura Stripe usando `Stripe-Signature` header
- Proteção contra replay attacks

### Rate Limiting
- Limite geral: 60 requisições/minuto
- Limite para assinaturas: 10 requisições/minuto
- Limpeza automática de contadores antigos

## 📊 Eventos Stripe Processados

### Assinaturas
- `customer.subscription.created` - Nova assinatura criada
- `customer.subscription.updated` - Assinatura atualizada
- `customer.subscription.deleted` - Assinatura cancelada
- `customer.subscription.trial_will_end` - Aviso de fim de trial

### Pagamentos
- `invoice.payment_succeeded` - Pagamento bem-sucedido
- `invoice.payment_failed` - Falha no pagamento

### Checkout
- `checkout.session.completed` - Checkout finalizado

## 🔄 Fluxo de Assinatura

### 1. Criação de Assinatura
```
Usuário → POST /api/subscriptions/create
       → SubscriptionService.createCheckoutSession()
       → Stripe Checkout Session
       → Redirect para Stripe
```

### 2. Processamento de Pagamento
```
Stripe → Webhook /api/webhooks/stripe
      → WebhookController.handleStripeWebhook()
      → SubscriptionService.processSubscriptionCreated()
      → Atualização no banco de dados
```

### 3. Gerenciamento de Assinatura
```
Usuário → GET /api/subscriptions/portal
       → SubscriptionService.createCustomerPortalSession()
       → Redirect para Stripe Customer Portal
```

## 🛠️ Configuração Necessária

### Variáveis de Ambiente
```properties
# Stripe Configuration
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# URLs de Redirecionamento
STRIPE_SUCCESS_URL=http://localhost:8080/subscription/success
STRIPE_CANCEL_URL=http://localhost:8080/subscription/cancel
```

### Webhooks no Stripe Dashboard
Configurar endpoint: `https://seu-dominio.com/api/webhooks/stripe`

Eventos necessários:
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.payment_succeeded`
- `invoice.payment_failed`
- `customer.subscription.trial_will_end`
- `checkout.session.completed`

## 🧪 Testes

### Testes Unitários
- `SubscriptionServiceTest` - Testa lógica de negócio
- Cobertura de cenários principais e casos de erro

### Testes de Integração
```bash
# Testar webhook
curl -X GET http://localhost:8080/api/webhooks/stripe/test

# Testar criação de assinatura (com JWT)
curl -X POST http://localhost:8080/api/subscriptions/create \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"planId": 1}'
```

## 📈 Monitoramento e Logs

### Logs Importantes
- Criação de sessões de checkout
- Processamento de webhooks
- Falhas de validação
- Rate limiting ativado

### Métricas Recomendadas
- Taxa de conversão de checkout
- Falhas de webhook
- Cancelamentos de assinatura
- Uso de rate limiting

## 🔧 Manutenção

### Limpeza Automática
- Contadores de rate limiting são limpos a cada 5 minutos
- Logs de webhook são mantidos para auditoria

### Backup e Recuperação
- Dados de assinatura são críticos
- Implementar backup regular da tabela `subscriptions`
- Manter logs de webhook para reprocessamento se necessário

## 🚀 Próximos Passos

1. **Implementar no Frontend**
   - Integrar com endpoints de assinatura
   - Implementar componentes de checkout
   - Adicionar portal do cliente

2. **Melhorias de Produção**
   - Migrar rate limiting para Redis
   - Implementar circuit breaker para Stripe
   - Adicionar métricas detalhadas

3. **Funcionalidades Avançadas**
   - Cupons de desconto
   - Upgrades/downgrades de plano
   - Relatórios de receita

## 📞 Suporte

Para dúvidas sobre a implementação:
- Verificar logs da aplicação
- Consultar documentação do Stripe
- Testar endpoints com Postman/curl

---

**Status**: ✅ Implementação Completa  
**Versão**: 1.0  
**Data**: $(date)