# Guia: Checkout Transparente para Assinaturas (Preapproval API)

Este documento descreve a **arquitetura recomendada** e o **passo a passo** para migrar de **Checkout Pro** para **Checkout Transparente** na integração com a API de Assinaturas do Mercado Pago (Preapproval), em um projeto único com backend Java/Spring e frontend Angular na pasta `src`.

---

## 1. Arquitetura recomendada (backend + Angular no mesmo projeto)

### Visão geral

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (Angular – src/Gerenciamento_de_..._front)  │
├─────────────────────────────────────────────────────────────────────────┤
│  • MercadoPago.js (Public Key) → apenas para criar card_token           │
│  • Formulário de cartão (CardForm) → coleta dados, gera token           │
│  • NÃO cria pagamentos nem assinaturas                                  │
│  • Envia card_token para o backend                                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ POST /api/subscription/checkout
                                    │ { planId, cardToken }
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Spring Boot – src/main/java)           │
├─────────────────────────────────────────────────────────────────────────┤
│  • Access Token do Mercado Pago                                         │
│  • Cria assinatura: POST /preapproval (card_token_id)                    │
│  • Associa assinatura ao usuário                                        │
│  • Processa webhooks (subscription_preapproval, payment)                  │
│  • Controla acesso com base no status da assinatura                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Webhooks
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         MERCADO PAGO                                    │
│  • Preapproval API (/preapproval)                                       │
│  • Notificações → seu backend                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### Regras de ouro

| Onde | O que usa | O que faz |
|------|-----------|-----------|
| **Frontend** | **Public Key** | Inicializa SDK, coleta dados do cartão, gera **card_token** (token único do cartão). Não envia dados de cartão ao backend. |
| **Backend** | **Access Token** | Cria/cancela assinaturas, consulta pagamentos, processa webhooks. Nunca expõe Access Token. |

- O frontend **nunca** chama a API do Mercado Pago para criar pagamento ou assinatura.
- O backend é o **único** responsável por criar assinatura, cancelar/pausar e processar webhooks.

---

## 2. Fluxo completo (Cadastro → Checkout Transparente → Assinatura)

### Passo a passo do usuário

1. **Cadastro**  
   Usuário se cadastra e faz login (fluxo já existente).

2. **Escolha do plano**  
   Acessa a tela de planos e escolhe um plano (ex.: plano mensal).

3. **Checkout Transparente (no Angular)**  
   - Tela de checkout na própria aplicação (sem sair do site).  
   - Formulário de cartão (número, validade, CVV, nome, documento, e-mail) usando **MercadoPago.js** (CardForm).  
   - Ao enviar o formulário, o SDK gera o **card_token** (representação segura do cartão).  
   - O frontend envia para o backend apenas: `planId` + `cardToken` (e não os dados do cartão).

4. **Backend cria a assinatura**  
   - Backend recebe `planId` e `cardToken`.  
   - Valida usuário e plano.  
   - Cria a assinatura local (status INCOMPLETE/PENDING).  
   - Chama a API do Mercado Pago: `POST /preapproval` com `card_token_id` (e demais campos: payer_email, reason, auto_recurring, etc.).  
   - Mercado Pago retorna o `preapproval_id` e status.  
   - Backend associa a assinatura ao usuário e persiste.  
   - Responde ao frontend com sucesso (e **sem** URL de redirecionamento).

5. **Confirmação no frontend**  
   - Frontend não redireciona para o Mercado Pago.  
   - Mostra mensagem de sucesso e pode redirecionar para uma página de “assinatura ativa” ou “obrigado”.

6. **Webhooks**  
   - Mercado Pago envia notificações (ex.: `subscription_preapproval`, `payment`) para a URL configurada no backend.  
   - Backend valida assinatura do webhook, atualiza status da assinatura e do pagamento.  
   - O acesso do usuário no sistema passa a refletir o status real (ativo, cancelado, pendente, etc.).

---

## 3. Passo a passo da implementação

### 3.1 Backend

#### 3.1.1 Configuração (Public Key + Access Token)

- **Access Token**: já usado no backend para chamar a API (criar preapproval, webhooks, etc.).  
- **Public Key**: necessária **apenas** no frontend para inicializar o MercadoPago.js e gerar o card_token.  
  - Configure no `.env` (ou equivalentes):
    - `MERCADOPAGO_TEST_PUBLIC_KEY` (teste)
    - `MERCADOPAGO_PROD_PUBLIC_KEY` (produção)
  - Exponha a Public Key ao frontend por um endpoint **somente leitura**, por exemplo:
    - `GET /api/mercadopago/public-key`  
    - Retorna a chave conforme o ambiente (test/prod). Não exponha o Access Token.

#### 3.1.2 Endpoint de checkout

- **Endpoint**: `POST /api/subscription/checkout`  
- **Autenticação**: Bearer JWT obrigatória.

**Checkout Transparente (dados da requisição):**  
O frontend deve enviar um **body JSON** com exatamente estes campos:

| Campo               | Tipo   | Obrigatório | Descrição |
|---------------------|--------|-------------|-----------|
| `planId`            | string | Sim         | ID do plano (local). |
| `cardTokenId`       | string | Sim         | Token do cartão retornado por `cardForm.getCardFormData().token`. |
| `payerEmail`        | string | Sim         | E-mail do pagador (formato válido). |
| `externalReference` | string | Sim         | ID do usuário logado (obtido por `GET /api/subscription/current-user-id`). |

Exemplo de body:
```json
{
  "planId": "1",
  "cardTokenId": "ff8080814a11b4b9014a11c2f3ab0001",
  "payerEmail": "comprador@email.com",
  "externalReference": "123"
}
```

Não enviar número do cartão, CVV ou qualquer dado sensível; apenas o token gerado pelo SDK.

- **Parâmetros** (resumo): `planId`, `cardTokenId`, `payerEmail`, `externalReference`.  
- **Fluxo**:
  1. Validar JWT e usuário.
  2. Validar plano (por ID local ou ID do plano no Mercado Pago).
  3. Criar registro de assinatura local (status inicial, ex.: INCOMPLETE/PENDING).
  4. Chamar `POST https://api.mercadopago.com/preapproval` com:
     - `payer_email`
     - `external_reference` (ex.: userId:planId:subscriptionId)
     - `card_token_id` = valor recebido como `cardToken`
     - `reason`, `auto_recurring` (e free_trial se aplicável)
     - `back_url` (opcional, para links de retorno se necessário)
  5. Tratar erros da API (cartão recusado, dados inválidos, etc.) e devolver mensagem amigável.
  6. Salvar `preapproval_id` na assinatura e associar ao usuário.
  7. Resposta de sucesso **sem** URL de redirecionamento (Checkout Transparente):
     - Ex.: `{ "success": true, "subscriptionId": 123, "preapprovalId": "...", "status": "pending" }`
     - Não retornar `checkoutUrl` / `init_point` nesse fluxo.

#### 3.1.3 Webhooks

- URL configurada no painel do Mercado Pago apontando para o backend (ex.: `https://seu-dominio.com/api/webhooks/mercadopago`).  
- Backend:
  - Valida o webhook (ex.: header de assinatura com `MERCADOPAGO_WEBHOOK_SECRET`).
  - Para evento `subscription_preapproval`: atualiza status da assinatura (pending, authorized, cancelled, etc.).
  - Para evento `payment`: pode atualizar pagamento vinculado à assinatura.  
- O controle de acesso (middleware/filter) deve usar o status da assinatura persistido no banco (não depender só do primeiro retorno da criação).

### 3.2 Frontend (Angular)

#### 3.2.1 Inclusão do SDK

- Incluir o script do Mercado Pago no `index.html`:
  ```html
  <script src="https://sdk.mercadopago.com/js/v2"></script>
  ```
- Ou usar o pacote npm `@mercadopago/sdk-js` e carregar antes de usar.

#### 3.2.2 Obter a Public Key

- Ao carregar a tela de checkout, chamar `GET /api/mercadopago/public-key`.
- Usar a chave retornada para instanciar o SDK: `new MercadoPago(publicKey)`.

#### 3.2.3 Formulário de cartão (CardForm) – dados do cartão para o pagamento

Para fazer o pagamento, o formulário **pega os dados do cartão** listados abaixo. O SDK do Mercado Pago usa esses dados só para gerar o **card token**; nenhum dado sensível é enviado ao seu backend.

**Dados do cartão coletados (conforme doc MP – Brasil):**

| Dado | Campo no form | Obrigatório |
|------|----------------|-------------|
| Número do cartão | `form-checkout__cardNumber` | Sim |
| Validade (MM/AA) | `form-checkout__expirationDate` | Sim |
| Código de segurança (CVV) | `form-checkout__securityCode` | Sim |
| Nome do titular | `form-checkout__cardholderName` | Sim |
| Banco emissor | `form-checkout__issuer` | Preenchido pelo SDK |
| Parcelas | `form-checkout__installments` | Preenchido pelo SDK |
| Tipo de documento | `form-checkout__identificationType` | Sim |
| Número do documento (CPF) | `form-checkout__identificationNumber` | Sim |
| E-mail do pagador | `form-checkout__cardholderEmail` | Sim |

- Usar o **CardForm** do MercadoPago.js: [Geração do card token - Assinaturas](https://www.mercadopago.com.br/developers/pt/docs/subscriptions/additional-content/cardtoken).
- No callback de submit, obter o token: `cardForm.getCardFormData()` → campo `token` (card_token); e o e-mail em `cardholderEmail`.
- Enviar ao backend **apenas**: `planId`, `cardTokenId` (valor de `token`), `payerEmail`, `externalReference`. Não enviar número de cartão, CVV ou documento.

#### 3.2.4 Chamada ao backend

- Ao receber o `token` do CardForm, enviar:
  - `POST /api/subscription/checkout` com `planId` e `cardToken` (o valor de `token`).
- Se a resposta for sucesso e **não** houver `checkoutUrl` (ou houver flag `transparentCheckout: true`):
  - Mostrar mensagem de sucesso.
  - Redirecionar para página de sucesso/assinatura (ex.: `/subscription/success` ou dashboard).
- Se houver erro (4xx/5xx), exibir mensagem retornada pelo backend (ex.: “Cartão recusado”, “Dados inválidos”).

### 3.3 Remoção do Checkout Pro

- Não usar mais:
  - Criação de Preference para redirecionar o usuário ao Mercado Pago para preencher o cartão.
  - Uso de `init_point` / `checkoutUrl` para abrir a página de pagamento do MP no fluxo de assinatura.
- Manter apenas o fluxo em que o frontend coleta o cartão, gera o card_token e o backend cria o preapproval com `card_token_id`.

---

## 4. Exemplo resumido do fluxo (Checkout Transparente)

```
1. Usuário logado acessa /plans/1 (detalhes do plano).
2. Clica em "Assinar" → abre o formulário de checkout (modal ou nova rota).
3. Preenche dados do cartão no CardForm (MercadoPago.js).
4. Submit → SDK gera card_token (token).
5. Frontend: POST /api/subscription/checkout?planId=1, body: { cardToken: token }.
6. Backend: cria assinatura local, POST /preapproval com card_token_id.
7. MP retorna preapproval_id e status.
8. Backend salva e responde: { success: true, subscriptionId, preapprovalId, status }.
9. Frontend: não redireciona para MP; mostra "Assinatura criada com sucesso" e redireciona para /subscription/success.
10. Webhooks atualizam status ao longo do tempo (autorizado, pago, cancelado, etc.).
```

---

## 5. Pontos de atenção e erros comuns

- **Não usar card_token mais de uma vez**: o token é de uso único e expira em pouco tempo (minutos). Cada tentativa de checkout deve gerar um novo token.
- **Public Key só no frontend**: nunca coloque a Public Key em código que rode no backend como “segredo”; ela é pública por design. Access Token **só** no backend.
- **HTTPS em produção**: tanto o frontend quanto a URL de webhook devem usar HTTPS em produção.
- **Validação de webhook**: sempre validar a assinatura do webhook com o `MERCADOPAGO_WEBHOOK_SECRET` para evitar requisições falsas.
- **Idempotência**: ao processar webhooks, usar `preapproval_id` / `payment_id` para evitar processar o mesmo evento duas vezes.
- **Tratamento de erros da API**: cartão recusado, dados inválidos ou limite excedido retornam erro da API; o backend deve mapear para mensagens claras (ex.: “Cartão recusado. Verifique os dados ou tente outro cartão.”).
- **Ambiente de teste**: use credenciais de teste (Public Key e Access Token de teste) e cartões de teste do Mercado Pago na documentação.

---

## 6. Referências à documentação oficial do Mercado Pago

- [Checkout Transparente – Visão geral](https://www.mercadopago.com.br/developers/pt/docs/checkout-api/landing)
- [Assinaturas – Visão geral](https://www.mercadopago.com.br/developers/pt/docs/subscriptions/landing)
- [Geração do card token (CardForm) – Assinaturas](https://www.mercadopago.com.br/developers/pt/docs/subscriptions/additional-content/cardtoken)
- [API Reference – Preapproval (criação de assinatura)](https://www.mercadopago.com.br/developers/pt/reference/preapproval/_preapproval/post)
- [Credenciais (Public Key e Access Token)](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/credentials)
- [Notificações (Webhooks)](https://www.mercadopago.com.br/developers/pt/docs/subscriptions/additional-content/your-integrations/notifications)

---

## 7. Correção de nomenclatura (card_token)

Na documentação e na API do Mercado Pago:

- No **frontend** (SDK): o objeto retornado pelo CardForm tem um campo chamado **`token`** (é o token do cartão).
- Na **API** de preapproval: o parâmetro enviado no body é **`card_token_id`** (valor desse token).

Ou seja: o frontend envia o “token” do cartão; o backend envia esse mesmo valor como **`card_token_id`** no POST `/preapproval`. Não há “card_token” como nome de campo na API; o nome correto é **card_token_id** no payload de criação da assinatura.

Este guia usa “card_token” no sentido de “token do cartão” (o valor) e “card_token_id” como o nome do parâmetro na API.
