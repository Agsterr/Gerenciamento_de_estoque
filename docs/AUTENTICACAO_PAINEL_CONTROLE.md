# Autenticação do Painel de Controle - Documentação para Frontend

Este documento explica como funciona a autenticação para o painel de controle administrativo.

**⚠️ NOTA:** Esta documentação é para ser usada por uma aplicação frontend **separada/externa**. A pasta `Gerenciamento_de_estoque_front` neste projeto não está sendo utilizada.

---

## 🔐 SISTEMA DE AUTENTICAÇÃO

O sistema já possui autenticação JWT completa e funcional. Você **NÃO precisa criar** um novo sistema de login. Basta usar o login existente com um usuário que tenha a role `ROLE_ADMIN`.

---

## 📋 COMO FUNCIONA

### 1. Login Existente

O endpoint de login já está configurado e funcionando:

**Endpoint:** `POST /auth/login`

**Body:**
```json
{
  "username": "seu_usuario_admin",
  "senha": "sua_senha",
  "orgId": 1
}
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2. Verificação de Role ADMIN

Após o login, o token JWT contém as roles do usuário. O sistema verifica automaticamente se o usuário tem a role `ROLE_ADMIN`.

**Como verificar se o usuário é admin:**
- O token JWT decodificado contém um campo `roles` com as roles do usuário
- Se `roles` contém `"ROLE_ADMIN"`, o usuário é admin
- O frontend já tem um `AdminGuard` que faz essa verificação automaticamente

---

## 🛡️ PROTEÇÃO DE ENDPOINTS

### Backend (Spring Security)

Todos os endpoints do painel de controle estão protegidos com `hasRole("ADMIN")`:

```java
// Endpoints protegidos no SecurityConfig.java
.requestMatchers(HttpMethod.POST, "/api/plans").hasRole("ADMIN")
.requestMatchers(HttpMethod.PUT, "/api/plans/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.DELETE, "/api/plans/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.POST, "/api/plans/sync-mercadopago").hasRole("ADMIN")
.requestMatchers(HttpMethod.PUT, "/api/orgs/**").hasRole("ADMIN")
.requestMatchers("/api/subscription/all").hasRole("ADMIN")
.requestMatchers("/roles/**").hasRole("ADMIN")
.requestMatchers("/admin/webhooks/failed/**").hasRole("ADMIN")
```

### Frontend (Implementação Necessária)

No seu frontend externo, você precisa implementar:

1. **Verificação de autenticação** antes de fazer requisições
2. **Verificação de role ADMIN** antes de exibir/acessar o painel
3. **Redirecionamento** para login se não estiver autenticado
4. **Redirecionamento** para home se não for admin

**Exemplo de verificação (pseudo-código):**
```typescript
// Verificar se usuário é admin antes de acessar painel
const user = getLoggedUser();
if (!user || !user.roles.includes('ROLE_ADMIN')) {
  redirectTo('/home');
}
```

---

## 🚀 COMO USAR NO PAINEL

### 1. Criar Usuário Admin (se ainda não tiver)

Se você ainda não tem um usuário com role ADMIN, você precisa:

1. **Criar a role ADMIN no banco** (se não existir):
```sql
INSERT INTO roles (nome) VALUES ('ROLE_ADMIN');
```

2. **Criar um usuário** via endpoint `/auth/register` ou diretamente no banco

3. **Associar a role ADMIN ao usuário**:
```sql
-- Supondo que o usuário tem id=1 e a role ADMIN tem id=1
INSERT INTO usuario_roles (usuario_id, roles_id) VALUES (1, 1);
```

### 2. Fazer Login

Use o endpoint de login existente com suas credenciais de admin:

```typescript
// Exemplo no frontend
this.authService.login({
  username: 'seu_usuario_admin',
  senha: 'sua_senha',
  orgId: 1
}).subscribe(response => {
  // Token será armazenado automaticamente no localStorage
  // Roles serão extraídas do token e armazenadas
  // Redirecionamento automático para /home
});
```

### 3. Acessar o Painel

Após o login, se o usuário tiver role `ROLE_ADMIN`:
- O `AdminGuard` permitirá acesso às rotas protegidas
- Todas as requisições para endpoints admin incluirão o token JWT no header
- O backend verificará automaticamente a role ADMIN

---

## 📝 ESTRUTURA DO TOKEN JWT

O token JWT contém as seguintes informações:

```json
{
  "sub": "username",
  "user_id": 1,
  "org_id": 1,
  "roles": ["ROLE_ADMIN", "ROLE_USER"],
  "exp": 1234567890
}
```

O campo `roles` é um array de strings. Para ser admin, o array deve conter `"ROLE_ADMIN"`.

---

## 🔍 VERIFICAÇÃO NO FRONTEND

O `AuthService` já possui métodos para verificar se o usuário é admin:

```typescript
// Verificar se está logado
this.authService.isLoggedIn(): boolean

// Obter usuário logado
this.authService.getLoggedUser(): {
  username: string,
  userId: number,
  orgId: number,
  roles: string[]
}

// Verificar se é admin (método no AppComponent)
this.isAdmin: boolean // Getter que verifica se tem ROLE_ADMIN
```

---

## 🛠️ IMPLEMENTAÇÃO NO FRONTEND

No seu frontend externo, você precisa implementar:

1. **Sistema de rotas** para o painel (ex: `/painel`, `/admin`, etc.)
2. **Guards/Proteção de rotas** que verificam role ADMIN
3. **Componentes** para cada seção do painel
4. **Serviços** para fazer requisições aos endpoints da API

**Estrutura sugerida de rotas:**
- `/painel` ou `/admin` - Rota principal do painel
- `/painel/planos` - Gestão de planos
- `/painel/organizacoes` - Gestão de organizações
- `/painel/roles` - Gestão de roles
- `/painel/assinaturas` - Gestão de assinaturas

**Todas as rotas devem verificar:**
- Se o usuário está autenticado (tem token JWT válido)
- Se o usuário tem a role `ROLE_ADMIN`

---

## ⚠️ IMPORTANTE

1. **Não precisa criar novo endpoint de login** - Use o existente (`/auth/login`)
2. **Não precisa criar novo sistema de autenticação** - JWT já está funcionando
3. **Apenas use um usuário com role `ROLE_ADMIN`** - O sistema já verifica automaticamente
4. **Todos os endpoints admin já estão protegidos** - Backend bloqueia acesso sem role ADMIN
5. **Frontend já tem guards prontos** - Apenas use o `AdminGuard` nas rotas

---

## 📚 REFERÊNCIAS

- **AuthService:** `Gerenciamento_de_estoque_front/src/app/services/auth.service.ts`
- **AdminGuard:** `Gerenciamento_de_estoque_front/src/app/guards/admin.guard.ts`
- **SecurityConfig:** `src/main/java/br/softsistem/Gerenciamento_de_estoque/config/SecurityConfig.java`
- **AuthController:** `src/main/java/br/softsistem/Gerenciamento_de_estoque/controller/AuthController.java`

---

## ✅ CHECKLIST PARA IMPLEMENTAR O PAINEL

- [x] Sistema de autenticação JWT funcionando
- [x] Endpoint de login (`/auth/login`) funcionando
- [x] Verificação de role ADMIN no backend
- [x] AdminGuard no frontend funcionando
- [x] Endpoints do painel protegidos no backend
- [ ] Criar componentes do painel no frontend
- [ ] Configurar rotas do painel com AdminGuard
- [ ] Criar interface do painel (UI)
- [ ] Testar login com usuário ADMIN
- [ ] Testar acesso às rotas protegidas

---

## 🎯 RESUMO

**Você já tem tudo pronto!** Apenas:

1. **Tenha um usuário com role `ROLE_ADMIN`** no banco de dados
2. **Faça login** usando o endpoint `/auth/login` existente
3. **Use o `AdminGuard`** nas rotas do painel no frontend
4. **Os endpoints já estão protegidos** no backend

Não é necessário criar novo sistema de autenticação!

