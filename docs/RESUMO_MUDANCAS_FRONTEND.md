# 📋 RESUMO RÁPIDO - MUDANÇAS PARA O FRONT-END

> **Para desenvolvedores Angular**  
> **Data:** 2025-01-15

---

## ✅ BOA NOTÍCIA

**O front-end NÃO precisa mudar nada!** Os endpoints continuam os mesmos.

---

## 🔄 O QUE MUDOU (Back-end)

### ❌ ANTES:
- Usava `Preference` (pagamento único)
- Não era assinatura recorrente real

### ✅ AGORA:
- Usa `PreapprovalPlan` (assinatura recorrente)
- É assinatura recorrente REAL que cobra mensalmente

---

## 📌 ENDPOINTS

| Endpoint | Método | Body? | Status |
|----------|--------|-------|--------|
| `/api/subscription/create?planId={id}` | POST | ❌ Não | ✅ Funciona |
| `/api/subscription/current` | GET | ❌ Não | ✅ Funciona |
| `/api/subscription/cancel` | POST | ❌ Não | ✅ Funciona |
| `/api/plans` | GET | ❌ Não | ✅ Funciona |
| `/api/plans` | POST | ✅ **SIM** | ✅ **NOVO!** |
| `/api/plans/{id}` | PUT | ✅ **SIM** | ✅ **NOVO!** |
| `/api/plans/{id}` | DELETE | ❌ Não | ✅ **NOVO!** |

---

## ⚠️ VERIFICAÇÕES NECESSÁRIAS

### 1. Service de Assinaturas
- [ ] Existe e usa `/api/subscription/*`
- [ ] Não chama Mercado Pago diretamente
- [ ] Trata erros (400, 401, 404, 500)

### 2. Componente de Seleção de Plano
- [ ] Redireciona para `checkoutUrl` retornada
- [ ] Trata retorno do checkout (success/cancel/pending)
- [ ] Consulta assinatura após retorno

### 3. Painel de Planos (se existir)
- [ ] Lista planos via `GET /api/plans`
- [ ] Não cria planos no Mercado Pago
- [ ] Cria planos apenas no banco de dados

---

## 📝 EXEMPLOS RÁPIDOS

### Criar Assinatura (POST sem body, só query param)
```typescript
// POST /api/subscription/create?planId=1
// Body: {} (vazio)
this.subscriptionService.createSubscription(planId).subscribe({
  next: (response) => {
    window.location.href = response.checkoutUrl;
  }
});
```

### Criar Plano (POST com body)
```typescript
// POST /api/plans
// Body: { name, price, type, ... }
this.planService.createPlan({
  name: 'Plano Premium',
  price: 99.90,
  type: 'PROFESSIONAL',
  maxUsers: 10,
  hasReports: true
}).subscribe({
  next: (plan) => {
    console.log('Plano criado:', plan);
  }
});
```

### Listar Planos (GET - não precisa de body)
```typescript
// GET /api/plans
// Body: não precisa
this.planService.getAllPlans().subscribe({
  next: (plans) => {
    console.log('Planos:', plans);
  }
});
```

---

## 📚 DOCUMENTAÇÃO COMPLETA

- **Guia Completo:** `docs/GUIA_FRONTEND_ASSINATURAS_RECORRENTES.md`
- **Criar Plano:** `docs/ENDPOINT_CRIAR_PLANO.md` ⭐ **NOVO!**
- **Webhooks:** `docs/VERIFICACAO_WEBHOOKS_PREAPPROVALPLAN.md`

---

**Resumo criado em:** 2025-01-15

