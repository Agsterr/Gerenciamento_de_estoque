# Endpoints para Painel de Controle - Documentação para Frontend

Este documento lista **todos os endpoints da API** necessários para criar um painel de controle administrativo no frontend.

**⚠️ NOTA:** Esta documentação é para ser usada por uma aplicação frontend **separada/externa**. A pasta `Gerenciamento_de_estoque_front` neste projeto não está sendo utilizada.

**Base URL:** `https://gerenciamento-de-estoque-mw08.onrender.com` (ou sua URL de desenvolvimento)

**Autenticação:** Todos os endpoints requerem JWT token no header:
```
Authorization: Bearer {token}
```

**⚠️ IMPORTANTE:** Todos os endpoints administrativos (criar, editar, deletar) requerem que o usuário tenha a role `ROLE_ADMIN`. O sistema já está configurado para verificar isso automaticamente.

**📚 Para mais detalhes sobre autenticação, consulte:** `docs/AUTENTICACAO_PAINEL_CONTROLE.md`

---

## 📋 ÍNDICE

1. [Planos do Mercado Pago](#1-planos-do-mercado-pago)
2. [Organizações](#2-organizações)
3. [Roles (Permissões)](#3-roles-permissões)
4. [Assinaturas](#4-assinaturas)

---

## 1. PLANOS DO MERCADO PAGO

### 1.1. Listar Todos os Planos
- **Método:** `GET`
- **Endpoint:** `/api/plans`
- **Autenticação:** Requerida
- **Descrição:** Retorna todos os planos ativos cadastrados
- **Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Básico",
    "description": "Plano básico com funcionalidades essenciais",
    "price": 29.90,
    "type": "BASIC",
    "maxUsers": 5,
    "maxProducts": 1000,
    "maxOrganizations": null,
    "hasReports": false,
    "hasAdvancedAnalytics": false,
    "hasApiAccess": false,
    "isActive": true,
    "stripePriceId": null,
    "stripeProductId": null
  }
]
```

### 1.2. Buscar Plano por ID
- **Método:** `GET`
- **Endpoint:** `/api/plans/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do plano
- **Resposta (200 OK):** Objeto Plan completo
- **Resposta (404 Not Found):** Plano não encontrado

### 1.3. Buscar Plano por Tipo
- **Método:** `GET`
- **Endpoint:** `/api/plans/type/{type}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `type` (string): `BASIC`, `PROFESSIONAL` ou `ENTERPRISE`
- **Resposta (200 OK):** Objeto Plan completo
- **Resposta (404 Not Found):** Plano não encontrado

### 1.4. Obter Recursos do Plano
- **Método:** `GET`
- **Endpoint:** `/api/plans/{id}/features`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do plano
- **Resposta (200 OK):**
```json
{
  "name": "Básico",
  "description": "Plano ideal para pequenas empresas...",
  "price": 29.90,
  "type": "BASIC",
  "maxUsers": 5,
  "maxProducts": 1000,
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false,
  "mercadoPagoReady": true
}
```

### 1.5. Sincronizar Planos com Mercado Pago
- **Método:** `POST`
- **Endpoint:** `/api/plans/sync-mercadopago`
- **Autenticação:** Requerida
- **Descrição:** Sincroniza todos os planos cadastrados com o Mercado Pago (cria produtos e preços no MP)
- **Resposta (200 OK):**
```json
{
  "status": "success",
  "message": "Planos sincronizados com sucesso"
}
```
- **Resposta (500 Internal Server Error):** Em caso de erro

### 1.6. ✅ CRIAR PLANO
- **Método:** `POST`
- **Endpoint:** `/api/plans`
- **Autenticação:** Requerida (Role: ADMIN)
- **Body:**
```json
{
  "name": "Nome do Plano",
  "description": "Descrição do plano",
  "price": 99.90,
  "type": "PROFESSIONAL",
  "maxUsers": 10,
  "maxProducts": 500,
  "hasReports": true,
  "hasAdvancedAnalytics": false,
  "hasApiAccess": false,
  "isActive": true
}
```
**⚠️ IMPORTANTE:** Não enviar `maxOrganizations` - organizações não são limitadas
- **Resposta (201 Created):** Objeto Plan criado

### 1.7. ✅ ATUALIZAR PLANO
- **Método:** `PUT`
- **Endpoint:** `/api/plans/{id}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Parâmetros Path:**
  - `id` (number): ID do plano
- **Body:** Mesmo formato do criar plano (campos opcionais)
- **Resposta (200 OK):** Objeto Plan atualizado

### 1.8. ✅ DELETAR/DESATIVAR PLANO
- **Método:** `DELETE`
- **Endpoint:** `/api/plans/{id}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Parâmetros Path:**
  - `id` (number): ID do plano
- **Resposta (204 No Content):** Sucesso

---

## 2. ORGANIZAÇÕES

### 2.1. Listar Todas as Organizações
- **Método:** `GET`
- **Endpoint:** `/api/orgs`
- **Autenticação:** Não requerida (mas recomendada)
- **Descrição:** Retorna todas as organizações cadastradas
- **Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "nome": "Nome da Organização",
    "ativo": true
  }
]
```

### 2.2. Buscar Organização por ID
- **Método:** `GET`
- **Endpoint:** `/api/orgs/{id}`
- **Autenticação:** Não requerida (mas recomendada)
- **Parâmetros Path:**
  - `id` (number): ID da organização
- **Resposta (200 OK):**
```json
{
  "id": 1,
  "nome": "Nome da Organização",
  "ativo": true
}
```
- **Resposta (404 Not Found):** Organização não encontrada

### 2.3. Criar Organização
- **Método:** `POST`
- **Endpoint:** `/api/orgs`
- **Autenticação:** Não requerida (mas recomendada)
- **Body:**
```json
{
  "nome": "Nome da Nova Organização"
}
```
- **Resposta (201 Created):**
```json
{
  "id": 1,
  "nome": "Nome da Nova Organização",
  "ativo": true
}
```
- **Resposta (409 Conflict):**
```json
{
  "error": "Já existe uma organização com este nome."
}
```

### 2.4. Atualizar Organização
- **Método:** `PUT`
- **Endpoint:** `/api/orgs/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da organização
- **Body:**
```json
{
  "nome": "Novo Nome da Organização"
}
```
- **Resposta (200 OK):** Objeto OrgDto atualizado
- **Resposta (404 Not Found):** Organização não encontrada
- **Resposta (409 Conflict):** Nome já existe

### 2.5. Bloquear/Desativar Organização
- **Método:** `PUT`
- **Endpoint:** `/api/orgs/{id}/desativar`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da organização
- **Body:** Vazio `{}`
- **Descrição:** Desativa uma organização (bloqueia acesso)
- **Resposta (204 No Content):** Sucesso
- **Resposta (404 Not Found):** Organização não encontrada

### 2.6. Desbloquear/Ativar Organização
- **Método:** `PUT`
- **Endpoint:** `/api/orgs/{id}/ativar`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da organização
- **Body:** Vazio `{}`
- **Descrição:** Ativa uma organização (libera acesso)
- **Resposta (204 No Content):** Sucesso
- **Resposta (404 Not Found):** Organização não encontrada

---

## 3. ROLES (PERMISSÕES)

### 3.1. Listar Todas as Roles
- **Método:** `GET`
- **Endpoint:** `/roles`
- **Autenticação:** Requerida
- **Descrição:** Retorna todas as roles/permissões cadastradas
- **Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "nome": "ADMIN"
  },
  {
    "id": 2,
    "nome": "USER"
  }
]
```
- **Resposta (204 No Content):** Quando não há roles

### 3.2. Buscar Role por ID
- **Método:** `GET`
- **Endpoint:** `/roles/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da role
- **Resposta (200 OK):**
```json
{
  "id": 1,
  "nome": "ADMIN"
}
```
- **Resposta (404 Not Found):** Role não encontrada

### 3.3. Criar Role
- **Método:** `POST`
- **Endpoint:** `/roles`
- **Autenticação:** Requerida (Role: ADMIN recomendado)
- **Body:**
```json
{
  "nome": "NOVA_ROLE"
}
```
- **Resposta (201 Created):**
```json
{
  "id": 3,
  "nome": "NOVA_ROLE"
}
```

### 3.4. Atualizar Role
- **Método:** `PUT`
- **Endpoint:** `/roles/{id}`
- **Autenticação:** Requerida (Role: ADMIN recomendado)
- **Parâmetros Path:**
  - `id` (number): ID da role
- **Body:**
```json
{
  "nome": "NOME_ATUALIZADO"
}
```
- **Resposta (200 OK):** Objeto Role atualizado
- **Resposta (404 Not Found):** Role não encontrada

### 3.5. Deletar Role
- **Método:** `DELETE`
- **Endpoint:** `/roles/{id}`
- **Autenticação:** Requerida (Role: ADMIN recomendado)
- **Parâmetros Path:**
  - `id` (number): ID da role
- **Resposta (204 No Content):** Sucesso
- **Resposta (404 Not Found):** Role não encontrada

---

## 4. ASSINATURAS

### 4.1. Obter Assinatura Atual do Usuário
- **Método:** `GET`
- **Endpoint:** `/api/subscription/current`
- **Autenticação:** Requerida
- **Descrição:** Retorna a assinatura ativa do usuário logado
- **Resposta (200 OK):**
```json
{
  "id": 1,
  "planId": 1,
  "planName": "Básico",
  "status": "ACTIVE",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-02-01T00:00:00"
}
```
- **Resposta (404 Not Found):** Usuário não possui assinatura

### 4.2. Criar Assinatura
- **Método:** `POST`
- **Endpoint:** `/api/subscription/create?planId={planId}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `planId` (number, obrigatório): ID do plano selecionado
- **Descrição:** Cria uma nova assinatura e retorna URL de checkout do Mercado Pago
- **Resposta (200 OK):**
```json
{
  "checkoutUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=...",
  "sessionId": "..."
}
```

### 4.3. Cancelar Assinatura
- **Método:** `POST`
- **Endpoint:** `/api/subscription/cancel`
- **Autenticação:** Requerida
- **Descrição:** Cancela a assinatura ativa do usuário
- **Resposta (200 OK):**
```json
{
  "message": "Assinatura cancelada com sucesso"
}
```

### 4.4. Obter URL do Portal do Cliente
- **Método:** `GET`
- **Endpoint:** `/api/subscription/customer-portal`
- **Autenticação:** Requerida
- **Descrição:** Retorna URL do portal do cliente do Mercado Pago
- **Resposta (200 OK):**
```json
{
  "portalUrl": "https://www.mercadopago.com.br/subscriptions/..."
}
```

### 4.5. Histórico de Assinaturas do Usuário
- **Método:** `GET`
- **Endpoint:** `/api/subscription/history`
- **Autenticação:** Requerida
- **Descrição:** Retorna histórico de todas as assinaturas do usuário
- **Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "planId": 1,
    "planName": "Básico",
    "status": "CANCELLED",
    "startDate": "2024-01-01T00:00:00",
    "endDate": "2024-02-01T00:00:00"
  }
]
```

### 4.6. Verificar Acesso a Funcionalidade
- **Método:** `GET`
- **Endpoint:** `/api/subscription/feature-access?feature={feature}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `feature` (string, obrigatório): Nome da funcionalidade (ex: "reports", "analytics", "api")
- **Resposta (200 OK):**
```json
{
  "hasAccess": true
}
```

### 4.7. Listar Todas as Assinaturas (Admin)
- **Método:** `GET`
- **Endpoint:** `/api/subscription/all`
- **Autenticação:** Requerida (Role: ADMIN)
- **Descrição:** Retorna todas as assinaturas do sistema (para painel admin)
- **Resposta (200 OK):** Array de SubscriptionDto

---

## 📝 NOTAS IMPORTANTES

### Endpoints que Precisam ser Criados no Backend

Os seguintes endpoints **NÃO EXISTEM** e precisam ser implementados no backend para o painel funcionar completamente:

1. **POST `/api/plans`** - Criar novo plano
2. **PUT `/api/plans/{id}`** - Atualizar plano existente
3. **DELETE `/api/plans/{id}`** ou **PUT `/api/plans/{id}/deactivate`** - Desativar/deletar plano

### Estrutura Sugerida para o Painel (Frontend)

1. **Dashboard Principal**
   - Estatísticas gerais (total de organizações, assinaturas ativas, etc.)
   - Gráficos e métricas

2. **Gestão de Planos**
   - Listar planos
   - Criar novo plano
   - Editar plano
   - Sincronizar com Mercado Pago
   - Ativar/Desativar plano

3. **Gestão de Organizações**
   - Listar todas as organizações
   - Ver detalhes da organização
   - Bloquear/Desbloquear organização
   - Editar organização

4. **Gestão de Roles**
   - Listar roles
   - Criar nova role
   - Editar role
   - Deletar role

5. **Gestão de Assinaturas**
   - Listar todas as assinaturas
   - Ver detalhes de assinatura
   - Cancelar assinatura (admin)

---

## 🔐 SEGURANÇA

- Todos os endpoints de criação/edição/deleção devem ter autenticação
- Endpoints administrativos devem verificar role `ADMIN`
- Validações de entrada devem ser implementadas no backend
- Logs de auditoria devem ser mantidos para ações administrativas

---

## 📚 REFERÊNCIAS

- Documentação completa: `docs/ENDPOINTS_API_FRONTEND.md`
- Manual Mercado Pago: `docs/MANUAL_FRONTEND_MERCADOPAGO.md`

