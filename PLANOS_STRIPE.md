# Planos de Assinatura - Integração Stripe

Este documento descreve os planos de assinatura implementados no sistema e sua integração com o Stripe.

## 📋 Planos Disponíveis

### 🟢 Plano Básico (R$ 29,90/mês)
**Ideal para pequenas empresas que estão começando**

- **Usuários**: Até 5 usuários
- **Produtos**: Até 1.000 produtos
- **Organizações**: 1 organização
- **Relatórios**: ✅ Relatórios básicos
- **Analytics**: ❌ Analytics avançado
- **API**: ❌ Acesso à API
- **Suporte**: Email

**Recursos inclusos:**
- Gerenciamento básico de estoque
- Controle de entrada e saída
- Relatórios de movimentação
- Alertas de estoque baixo
- Suporte por email

---

### 🟡 Plano Profissional (R$ 79,90/mês)
**Completo para empresas em crescimento**

- **Usuários**: Até 25 usuários
- **Produtos**: Até 10.000 produtos
- **Organizações**: Até 5 organizações
- **Relatórios**: ✅ Relatórios completos
- **Analytics**: ✅ Analytics avançado
- **API**: ❌ Acesso à API
- **Suporte**: Prioritário

**Recursos inclusos:**
- Todos os recursos do plano Básico
- Dashboard com analytics avançado
- Relatórios personalizados
- Previsão de demanda
- Integração com fornecedores
- Suporte prioritário

---

### 🔴 Plano Empresarial (R$ 199,90/mês)
**Solução enterprise com recursos ilimitados**

- **Usuários**: Ilimitados
- **Produtos**: Ilimitados
- **Organizações**: Ilimitadas
- **Relatórios**: ✅ Relatórios completos
- **Analytics**: ✅ Analytics avançado
- **API**: ✅ Acesso completo à API
- **Suporte**: Dedicado 24/7

**Recursos inclusos:**
- Todos os recursos do plano Profissional
- API REST completa
- Webhooks personalizados
- Integrações avançadas
- Relatórios customizados
- Suporte dedicado 24/7
- Gerente de conta dedicado

## 🔧 Integração com Stripe

### Configuração Automática

O sistema sincroniza automaticamente os planos com o Stripe na inicialização:

1. **Produtos Stripe**: Criados automaticamente para cada plano
2. **Preços Stripe**: Configurados em BRL com cobrança mensal
3. **Metadata**: Inclui informações sobre limites e recursos

### Endpoints da API

#### Listar Planos
```http
GET /api/plans
```
Retorna todos os planos ativos.

#### Buscar Plano por ID
```http
GET /api/plans/{id}
```

#### Buscar Plano por Tipo
```http
GET /api/plans/type/{type}
```
Tipos disponíveis: `BASIC`, `PROFESSIONAL`, `ENTERPRISE`

#### Recursos do Plano
```http
GET /api/plans/{id}/features
```
Retorna informações detalhadas sobre recursos e limites.

#### Validações de Limite

**Validar Usuários:**
```http
GET /api/plans/{id}/validate/user?currentUserCount=3
```

**Validar Produtos:**
```http
GET /api/plans/{id}/validate/product?currentProductCount=500
```

**Validar Organizações:**
```http
GET /api/plans/{id}/validate/organization?currentOrgCount=2
```

#### Sincronização Manual
```http
POST /api/plans/sync-stripe
```
Força a sincronização de todos os planos com o Stripe.

## 🛠️ Implementação Técnica

### Estrutura do Banco

A tabela `plans` contém:
- Informações básicas (nome, descrição, preço)
- Limites por recurso (usuários, produtos, organizações)
- Flags de recursos (relatórios, analytics, API)
- IDs de integração Stripe (product_id, price_id)

### Serviços

**PlanService**: Gerencia lógica de negócio dos planos
- Validação de limites
- Sincronização com Stripe
- Verificação de recursos

**PlanController**: Endpoints REST para gerenciamento
- CRUD de planos
- Validações de limite
- Informações de recursos

### Inicialização

**PlanInitializer**: Componente que executa na inicialização
- Sincroniza planos com Stripe automaticamente
- Trata erros graciosamente
- Permite funcionamento offline

## 🔐 Segurança e Validações

### Validação de Limites

O sistema valida automaticamente:
- Número máximo de usuários por organização
- Número máximo de produtos cadastrados
- Número máximo de organizações por conta

### Controle de Acesso

Recursos são controlados por flags:
- `hasReports`: Acesso a relatórios
- `hasAdvancedAnalytics`: Analytics avançado
- `hasApiAccess`: Acesso à API REST

## 📊 Monitoramento

### Logs

O sistema registra:
- Sincronização com Stripe
- Criação de produtos/preços
- Erros de integração
- Validações de limite

### Métricas

Acompanhe:
- Uso por plano
- Limites atingidos
- Upgrades/downgrades
- Cancelamentos

## 🚀 Próximos Passos

1. **Configurar Stripe**: Adicionar chaves reais no `.env`
2. **Testar Integração**: Verificar criação de produtos/preços
3. **Implementar Checkout**: Integrar com Stripe Checkout
4. **Webhooks**: Configurar eventos de assinatura
5. **Portal do Cliente**: Permitir gerenciamento de assinatura

## 📞 Suporte

Para dúvidas sobre a implementação:
- Consulte os logs da aplicação
- Verifique a documentação do Stripe
- Use o endpoint `/api/plans/sync-stripe` para reprocessar