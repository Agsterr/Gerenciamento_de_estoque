# Como ativar e usar o modo Sandbox (teste) – Mercado Pago

Resumo com base na [documentação oficial do Mercado Pago](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/test/accounts) e em [Credenciais](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/credentials).

---

## 1. Não existe “ativar sandbox” – use credenciais de teste

O Mercado Pago **não tem um botão “ativar sandbox”**. O modo teste é o uso das **credenciais de teste** da sua aplicação.

- **Credenciais de teste** → ambiente de **teste/sandbox** (simulação, sem dinheiro real).
- **Credenciais de produção** → ambiente **produção** (pagamentos reais).

As credenciais de teste **já vêm ativas** quando você cria a aplicação. Não é preciso ativar nada.

---

## 2. Onde pegar as credenciais de teste

1. Acesse **[Suas integrações](https://www.mercadopago.com.br/developers/panel/app)** e faça login.
2. Clique na sua **aplicação**.
3. No menu à esquerda: **Testes** → **Credenciais de teste**.
4. Você verá:
   - **Public Key** (começa com `TEST-`) – uso no frontend, se precisar.
   - **Access Token** (começa com `TEST-`) – uso no **backend** (nunca exponha no front).

No backend deste projeto, use no `.env`:

- `MERCADOPAGO_ENVIRONMENT=test`
- `MERCADOPAGO_TEST_ACCESS_TOKEN=TEST-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

Assim o backend já opera em modo teste (sandbox).

---

## 3. Contas de teste (para simular o comprador)

Para **testar o fluxo de compra** (incluindo checkout de assinaturas) sem cobrança real:

1. Em **Suas integrações** → sua aplicação → **Contas de teste**.
2. Clique em **+ Criar conta de teste**.
3. Crie pelo menos:
   - **Vendedor** – sua conta real já atua como vendedor ao usar as credenciais.
   - **Comprador** – use esta conta para simular quem está assinando.
4. Anote **Usuário** e **Senha** da conta **Comprador**.

No checkout do Mercado Pago, faça login com essa conta **Comprador de teste** e use um **cartão de teste** (veja link abaixo). Assim o pagamento é simulado e não gera cobrança real.

- [Contas de teste – Documentação](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/test/accounts)
- [Cartões de teste](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/test/cards)

---

## 4. Credenciais de produção (quando for para produção)

Para receber pagamentos reais:

1. Em **Suas integrações** → sua aplicação → **Produção** → **Credenciais de produção**.
2. Siga o fluxo **“Ativar credenciais de produção”** (indústria, website, termos, etc.).
3. Depois de ativadas, use no `.env`:
   - `MERCADOPAGO_ENVIRONMENT=production`
   - `MERCADOPAGO_PROD_ACCESS_TOKEN=APP_USR-xxxxxxxx...`

---

## 5. Resumo para este projeto

| Objetivo              | Configuração |
|-----------------------|-------------|
| Desenvolvimento/teste | `MERCADOPAGO_ENVIRONMENT=test` e `MERCADOPAGO_TEST_ACCESS_TOKEN=TEST-...` |
| Produção              | `MERCADOPAGO_ENVIRONMENT=production` e `MERCADOPAGO_PROD_ACCESS_TOKEN=APP_USR-...` |
| Testar compra         | Conta de teste **Comprador** + cartão de teste na página do MP |

Não existe URL separada “sandbox” para o checkout de assinaturas: a mesma URL (`www.mercadopago.com.br/subscriptions/checkout?preapproval_plan_id=...`) é usada; o fato de ser teste ou produção depende das **credenciais** (TEST- vs APP_USR-) e do **plano** criado com essas credenciais.
