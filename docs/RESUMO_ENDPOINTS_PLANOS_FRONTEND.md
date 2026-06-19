# 📋 RESUMO: ENDPOINTS DE PLANOS PARA FRONT-END

> **Documento resumido para passar ao front-end**  
> **Data:** 2025-01-15

---

## 🎯 OBJETIVO

Quando o usuário seleciona um **tipo de plano** (Básico, Profissional ou Empresarial) no formulário, o front-end deve **pré-preencher automaticamente** todos os campos com os valores do plano correspondente que já está no banco de dados.

---

## 🔌 ENDPOINTS PRINCIPAIS

### 1. Buscar Todos os Planos
```
GET /api/plans
```
**Retorna:** Lista com os 3 planos pré-definidos (Básico, Profissional, Empresarial)

### 2. Buscar Plano por Tipo
```
GET /api/plans/type/{type}
```
**Parâmetros:**
- `type`: `BASIC`, `PROFESSIONAL` ou `ENTERPRISE`

**Retorna:** Plano completo do tipo especificado

### 3. Criar Plano no Mercado Pago
```
POST /api/plans
```
**Body:** Dados do plano (já pré-preenchidos do formulário)

---

## 📊 PLANOS PRÉ-DEFINIDOS

### 🟢 Básico (BASIC)
```json
{
  "name": "Básico",
  "description": "Plano ideal para pequenas empresas que estão começando...",
  "price": 29.90,
  "type": "BASIC",
  "maxUsers": 5,
  "maxProducts": 1000,
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false
}
```

### 🟡 Profissional (PROFESSIONAL)
```json
{
  "name": "Profissional",
  "description": "Plano completo para empresas em crescimento...",
  "price": 79.90,
  "type": "PROFESSIONAL",
  "maxUsers": 25,
  "maxProducts": 10000,
  "hasReports": true,
  "hasAdvancedAnalytics": true,
  "hasApiAccess": false
}
```

### 🔴 Empresarial (ENTERPRISE)
```json
{
  "name": "Empresarial",
  "description": "Solução enterprise com recursos ilimitados...",
  "price": 199.90,
  "type": "ENTERPRISE",
  "maxUsers": null,
  "maxProducts": null,
  "hasReports": true,
  "hasAdvancedAnalytics": true,
  "hasApiAccess": true
}
```

**Nota:** `null` = ilimitado

---

## 💻 FLUXO RECOMENDADO

1. **Ao carregar formulário:**
   ```typescript
   GET /api/plans
   // Armazenar planos em memória
   ```

2. **Ao selecionar tipo de plano:**
   ```typescript
   // Buscar plano do tipo selecionado
   const plan = plans.find(p => p.type === selectedType);
   
   // Pré-preencher formulário
   form.patchValue({
     name: plan.name,
     description: plan.description,
     price: plan.price,
     maxUsers: plan.maxUsers,
     maxProducts: plan.maxProducts,
     hasReports: plan.hasReports,
     hasAdvancedAnalytics: plan.hasAdvancedAnalytics,
     hasApiAccess: plan.hasApiAccess
   });
   ```

3. **Ao criar no Mercado Pago:**
   ```typescript
   POST /api/plans
   // Enviar dados do formulário
   ```

---

## ⚠️ IMPORTANTE

- **NÃO incluir** campo `maxOrganizations` - organizações não são limitadas
- **NÃO incluir** campos `id`, `stripePriceId`, `stripeProductId`, `createdAt`, `updatedAt` no POST
- Campos `maxUsers` e `maxProducts` podem ser `null` (ilimitado)

---

## 📚 DOCUMENTAÇÃO COMPLETA

Para documentação completa com exemplos de código, consulte:
- **`docs/MANUAL_FRONTEND_PLANOS.md`** - Manual completo com exemplos TypeScript/Angular

---

**Documento criado em:** 2025-01-15







