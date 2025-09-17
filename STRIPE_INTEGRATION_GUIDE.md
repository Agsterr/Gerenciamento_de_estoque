# Guia de Integração com Stripe

## Visão Geral

Este documento descreve como configurar e usar a integração completa com Stripe para sistema de assinaturas com teste gratuito de 14 dias, planos recorrentes mensais e notificações por email.

## Funcionalidades Implementadas

### ✅ Sistema de Planos
- 3 planos pré-configurados (Básico, Profissional, Empresarial)
- Diferentes limites por plano (usuários, produtos, organizações)
- Funcionalidades específicas (relatórios, analytics, API)
- Preços mensais recorrentes

### ✅ Sistema de Assinaturas
- Teste gratuito de 14 dias automático
- Conversão de trial para assinatura paga
- Cancelamento de assinaturas
- Verificação de acesso a funcionalidades
- Histórico completo de assinaturas

### ✅ Sistema de Pagamentos
- Integração completa com Stripe
- Checkout sessions para trials e pagamentos
- Webhooks para confirmação de pagamentos
- Histórico de pagamentos e falhas
- Portal de cobrança do cliente

### ✅ Sistema de Notificações
- Email de boas-vindas ao trial
- Alerta de fim de trial (3 dias antes)
- Confirmação de conversão para pago
- Notificação de cancelamento
- Alertas de falha de pagamento

### ✅ Tarefas Agendadas
- Verificação de trials próximos do fim (a cada 6 horas)
- Processamento de trials expirados (diariamente às 02:00)
- Relatórios e limpeza (opcionais)

## Configuração

### 1. Variáveis de Ambiente

Configure as seguintes variáveis no arquivo `.env`:

```env
# Stripe Configuration
STRIPE_PUBLISHABLE_KEY=pk_test_sua_chave_publica_stripe_aqui
STRIPE_SECRET_KEY=sk_test_sua_chave_secreta_stripe_aqui
STRIPE_WEBHOOK_SECRET=whsec_sua_chave_webhook_stripe_aqui
STRIPE_SUCCESS_URL=http://localhost:8080/subscription/success
STRIPE_CANCEL_URL=http://localhost:8080/subscription/cancel

# Email Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=seu_email@gmail.com
SMTP_PASSWORD=sua_senha_app_gmail
EMAIL_FROM=seu_email@gmail.com
EMAIL_FROM_NAME=Sistema de Gerenciamento de Estoque
```

### 2. Configuração do Stripe

1. **Criar conta no Stripe**: https://stripe.com
2. **Obter chaves de API** no dashboard do Stripe
3. **Criar produtos e preços** no Stripe para cada plano
4. **Configurar webhook** apontando para: `https://seu-dominio.com/api/webhooks/stripe`
5. **Eventos do webhook** necessários:
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`

### 3. Configuração de Email

1. **Gmail**: Criar senha de app nas configurações de segurança
2. **Outros provedores**: Configurar SMTP conforme documentação

## Endpoints da API

### Planos
- `GET /api/plans` - Listar todos os planos
- `GET /api/plans/{id}` - Buscar plano específico
- `GET /api/plans/public` - Planos para página pública

### Assinaturas
- `POST /api/subscriptions/trial` - Iniciar trial gratuito
- `POST /api/subscriptions/{id}/upgrade` - Converter trial em pago
- `POST /api/subscriptions/{id}/cancel` - Cancelar assinatura
- `GET /api/subscriptions/current` - Assinatura atual do usuário
- `GET /api/subscriptions/history` - Histórico de assinaturas
- `GET /api/subscriptions/feature/{feature}` - Verificar acesso a funcionalidade
- `GET /api/subscriptions/stripe-config` - Configuração pública do Stripe

### Webhooks
- `POST /api/webhooks/stripe` - Processar eventos do Stripe

## Fluxo de Uso

### 1. Iniciar Trial
```bash
POST /api/subscriptions/trial
{
  "planId": 1
}
```

### 2. Verificar Status da Assinatura
```bash
GET /api/subscriptions/current
```

### 3. Converter Trial em Pago
```bash
POST /api/subscriptions/{subscriptionId}/upgrade
```

### 4. Verificar Acesso a Funcionalidade
```bash
GET /api/subscriptions/feature/reports
GET /api/subscriptions/feature/advanced_analytics
GET /api/subscriptions/feature/api_access
```

### 5. Cancelar Assinatura
```bash
POST /api/subscriptions/{subscriptionId}/cancel
```

## Estrutura do Banco de Dados

### Tabela `plans`
- Armazena os planos disponíveis
- Inclui limites e funcionalidades
- Referências para produtos/preços do Stripe

### Tabela `subscriptions`
- Assinaturas dos usuários
- Status, datas de trial e período atual
- Referências para Stripe

### Tabela `payments`
- Histórico de pagamentos
- Status e detalhes de falhas
- Referências para Stripe

## Monitoramento e Logs

### Logs Importantes
- Criação de assinaturas
- Processamento de webhooks
- Envio de emails
- Execução de tarefas agendadas

### Métricas Sugeridas
- Taxa de conversão de trial para pago
- Churn rate (cancelamentos)
- Revenue mensal recorrente (MRR)
- Falhas de pagamento

## Segurança

### Validações Implementadas
- Validação de webhooks do Stripe
- Verificação de propriedade de assinatura
- Sanitização de dados de entrada
- Logs de segurança

### Boas Práticas
- Nunca expor chaves secretas no frontend
- Validar todos os webhooks
- Implementar rate limiting
- Monitorar tentativas de fraude

## Troubleshooting

### Problemas Comuns

1. **Webhook não funciona**
   - Verificar URL do webhook no Stripe
   - Confirmar secret do webhook
   - Verificar logs de erro

2. **Email não enviado**
   - Verificar configurações SMTP
   - Confirmar senha de app (Gmail)
   - Verificar logs de email

3. **Pagamento falha**
   - Verificar configuração do Stripe
   - Confirmar produtos/preços criados
   - Verificar logs de pagamento

### Logs de Debug
```bash
# Verificar logs da aplicação
tail -f logs/application.log | grep -i stripe
tail -f logs/application.log | grep -i subscription
tail -f logs/application.log | grep -i email
```

## Próximos Passos

### Melhorias Sugeridas
1. **Dashboard administrativo** para gerenciar assinaturas
2. **Métricas e analytics** de assinaturas
3. **Cupons de desconto** e promoções
4. **Planos anuais** com desconto
5. **Upgrade/downgrade** entre planos
6. **Integração com sistema de suporte**

### Testes Recomendados
1. **Teste completo do fluxo** de trial para pago
2. **Teste de cancelamento** e reativação
3. **Teste de falha de pagamento** e recuperação
4. **Teste de webhooks** em ambiente de produção
5. **Teste de emails** em diferentes provedores

---

**Nota**: Este sistema está pronto para produção, mas recomenda-se testes extensivos em ambiente de staging antes do deploy final.