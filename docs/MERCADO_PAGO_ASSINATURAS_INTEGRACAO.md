# Integração de Assinaturas – Mercado Pago

Resumo da documentação oficial do Mercado Pago para integrar **Assinaturas** (pagamentos recorrentes com API).

**Documentação oficial:**  
- [Assinaturas – Visão geral](https://www.mercadopago.com.br/developers/pt/docs/subscriptions/landing)  
- [Assinaturas com plano associado](https://www.mercadopago.com.br/developers/pt/docs/subscriptions/integration-configuration/subscription-associated-plan)  
- [Referência API – POST /preapproval](https://www.mercadopago.com.br/developers/pt/reference/subscriptions/_preapproval/post)  
- [Referência API – POST /preapproval_plan](https://www.mercadopago.com.br/developers/pt/reference/subscriptions/_preapproval_plan/post)  
- [Notificações (Webhooks)](https://www.mercadopago.com.br/developers/pt/docs/subscriptions/additional-content/your-integrations/notifications)

---

## 1. O que são Assinaturas no Mercado Pago

- **Cobranças recorrentes** automatizadas (semanal, mensal ou anual).
- Cliente assina e o Mercado Pago cobra conforme a frequência definida.
- Você cria a assinatura via API e envia o **link de pagamento**; o cliente paga no ambiente do Mercado Pago.
- Após o primeiro pagamento, as cobranças seguintes são automáticas.

**Recursos:** período de teste, tentativas automáticas se a cobrança falhar, Pix/cartão/boleto, prevenção de fraudes.

---

## 2. Dois fluxos de integração

### Opção A: Assinatura **com plano associado** (recomendado no seu caso)

1. **Criar o plano** (uma vez) – `POST /preapproval_plan`  
   - Define valor, frequência, trial, moeda etc.  
   - Na resposta, o **`id`** é o **`preapproval_plan_id`** (usado na criação da assinatura).  
   - Você pode criar o plano no painel do Mercado Pago e usar só esse ID.

2. **Criar a assinatura** – `POST /preapproval`  
   - Envia `preapproval_plan_id` (ID do plano já criado).  
   - Envia `payer_email`, `external_reference`, `back_url`.  
   - Opcional: `card_token_id` e `status: "authorized"` se o pagamento for aprovado na hora.  
   - Resposta traz **`init_point`** (URL para o cliente pagar).  
   - Redirecione o usuário para essa URL.

### Opção B: Assinatura **sem plano associado**

- Você envia tudo no `POST /preapproval`: `reason`, `external_reference`, `payer_email`, `auto_recurring` (valor, frequência, moeda, etc.), `back_url`.  
- Não usa `preapproval_plan_id`.

---

## 3. Endpoints principais

| Ação              | Método | Endpoint                          | Uso no seu sistema                          |
|-------------------|--------|-----------------------------------|---------------------------------------------|
| Criar plano       | POST   | `/preapproval_plan`               | Opcional (se criar plano via API)           |
| **Criar assinatura** | POST   | **`/preapproval`**                | **Usado** – vincula usuário ao plano        |
| Atualizar assinatura | PUT    | `/preapproval/{id}`               | Cancelar, pausar, etc.                       |
| Buscar assinatura | GET    | `/preapproval/{id}`               | Consultar status                             |

**Base URL:** `https://api.mercadopago.com`  
**Header obrigatório:** `Authorization: Bearer SEU_ACCESS_TOKEN`

---

## 4. Criar assinatura (POST /preapproval) – com plano já criado

Quando o plano já existe no painel (ou foi criado via API), você só chama:

```http
POST https://api.mercadopago.com/preapproval
Authorization: Bearer SEU_ACCESS_TOKEN
Content-Type: application/json
```

**Body (exemplo):**

```json
{
  "preapproval_plan_id": "2c938084726fca480172750000000000",
  "reason": "Plano Premium Mensal",
  "external_reference": "123",
  "payer_email": "cliente@email.com",
  "back_url": "https://seusite.com/assinatura/sucesso",
  "status": "pending"
}
```

- **`preapproval_plan_id`**: ID do plano no Mercado Pago (painel ou resposta do `POST /preapproval_plan`).  
- **`external_reference`**: identificador no seu sistema (ex.: ID do usuário). Obrigatório para vincular assinatura ao usuário.  
- **`payer_email`**: e-mail do pagador.  
- **`back_url`**: URL de retorno após o pagamento.  
- **`status`**: `"pending"` (cliente paga no link) ou `"authorized"` (quando envia `card_token_id` e o pagamento já é aprovado).

**Resposta (exemplo):**

- `id`: ID da assinatura (preapproval_id).  
- `init_point`: URL para redirecionar o cliente a pagar.  
- `status`, `date_created`, etc.

Seu backend já usa esse fluxo em `MercadoPagoService.createPreapproval()`.

---

## 5. Processo para o cliente

1. Cliente escolhe o plano no seu site.  
2. Seu backend chama `POST /preapproval` com `preapproval_plan_id`, `payer_email`, `external_reference`, `back_url`.  
3. Backend recebe `init_point` e devolve ao front.  
4. Front redireciona o cliente para `init_point` (página de pagamento do Mercado Pago).  
5. Cliente paga (cartão, Pix, boleto, etc.) no Mercado Pago.  
6. Mercado Pago envia **webhooks** para sua API (criação/atualização da assinatura, pagamentos autorizados).  
7. Você atualiza o status no seu sistema (liberar/bloquear usuário) conforme os eventos.

---

## 6. Notificações (Webhooks)

Configure em **Suas integrações** no painel do Mercado Pago:

| Tópico                        | Uso em Assinaturas                          |
|------------------------------|---------------------------------------------|
| **Pagamentos** (`payment`)   | Pagamentos da assinatura (inclusive recorrência). |
| **Planos e assinaturas**     |                                             |
| `subscription_preapproval`   | Criação/atualização da assinatura (vinculação ao plano). |
| `subscription_authorized_payment` | Pagamento recorrente autorizado (cobrança mensal, etc.). |

**Tipo recomendado:** **Webhooks** (com assinatura secreta no header `x-signature`), não IPN.

**URL do webhook:** a que o Mercado Pago vai chamar (ex.: `https://seu-dominio.com/api/webhooks/mercadopago`).  
Documentação: [Webhooks](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks).

---

## 7. Requisitos prévios

- **Conta de vendedor** no Mercado Pago.  
- **Aplicação** criada em [Suas integrações](https://www.mercadopago.com.br/developers/pt/docs/subscriptions/additional-content/your-integrations/introduction).  
- **Credenciais**: Access Token (produção ou teste).  
- **Plano** já criado (painel ou `POST /preapproval_plan`) para usar `preapproval_plan_id`.

---

## 8. Resumo do fluxo no seu sistema

1. **Plano:** criado no painel do Mercado Pago; você guarda o **ID do plano** (`preapproval_plan_id`) no seu cadastro de planos.  
2. **Assinatura:** usuário escolhe plano → backend chama `POST /preapproval` com `preapproval_plan_id`, `payer_email`, `external_reference` = ID do usuário, `back_url` → recebe `init_point` → front redireciona para o Mercado Pago.  
3. **Webhooks:** sua API recebe em `POST /api/webhooks/mercadopago` os eventos `subscription_preapproval` e `payment` / `subscription_authorized_payment`; você identifica o usuário pelo `external_reference` e libera ou bloqueia o acesso conforme o status (authorized/approved vs cancelled).

Isso está alinhado com a documentação oficial de Assinaturas do Mercado Pago e com o fluxo já implementado no seu backend e frontend.
