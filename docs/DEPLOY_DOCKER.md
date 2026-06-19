# Deploy com Docker — Guia completo

Este documento descreve como subir, atualizar e operar todos os containers do **Gerenciamento de Estoque**, tanto em ambiente local quanto em produção (servidor Hetzner).

---

## Visão geral da arquitetura

O `docker-compose.yml` na raiz do repositório orquestra **4 serviços**:

| Container | Imagem / build | Função |
|-----------|----------------|--------|
| `gerenciamento-postgres` | `postgres:15-alpine` | Banco PostgreSQL (`gerenciamento_estoque`) |
| `gerenciamento-app` | Build do `Dockerfile` (Spring Boot) | API backend (porta interna 8080) |
| `gerenciamento-nginx` | Build do `docker/nginx/Dockerfile` | Frontend Angular + proxy reverso (porta 80) |
| `gerenciamento-cloudflared` | `cloudflare/cloudflared` | Túnel Cloudflare para expor o app na internet |

### Fluxo de uma requisição

```
Internet
   │
   ▼
cloudflared (túnel Cloudflare → porta 80 do host)
   │
   ▼
nginx :80
   ├── /auth, /produtos, /categorias, ...  →  proxy  →  app:8080 (Spring Boot)
   └── /, /login, /dashboard, ...          →  arquivos estáticos Angular
```

### O que cada build faz

**Backend (`Dockerfile`)**
1. Estágio Maven: compila o JAR (`demo.jar`) a partir de `src/main`
2. Estágio runtime: JRE 17 Alpine + JAR
3. No startup: Flyway aplica migrations em `src/main/resources/db/migration/`

**Frontend + Nginx (`docker/nginx/Dockerfile`)**
1. Estágio Node: `npm ci` + `npm run build -- --configuration=docker`
2. Usa `environment.docker.ts` (`apiUrl: ''`) — a API é chamada no mesmo host via nginx
3. Estágio Nginx: copia o build para `/usr/share/nginx/html` e aplica `default.conf`

---

## Pré-requisitos

### Local (Windows / Linux / macOS)

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) ou Docker Engine + Compose v2
- Git
- (Opcional) Java 17 e Node 20 — só se for rodar backend/frontend **fora** do Docker

### Produção (Hetzner)

- Servidor Ubuntu com Docker e Docker Compose
- Acesso SSH por chave (alias `hetzner` — ver seção [SSH](#acesso-ssh-ao-servidor))
- Arquivo `.env` configurado no servidor (não versionado)
- Token do túnel Cloudflare em `CLOUDFLARED_TUNNEL_TOKEN`
- Chave Asaas em `docker/secrets/asaas_sandbox_api_key.txt`

---

## Configuração inicial (primeira vez)

### 1. Clonar o repositório

```powershell
git clone <url-do-repositorio> Gerenciamento_de_estoque
cd Gerenciamento_de_estoque
```

### 2. Criar o arquivo `.env`

Copie o exemplo e ajuste os valores:

```powershell
copy .env.example .env
```

Variáveis **obrigatórias** para o stack subir com segurança:

| Variável | Descrição |
|----------|-----------|
| `JWT_SECRET` | Segredo JWT (mín. 256 bits) |
| `APP_PUBLIC_URL` | URL pública do app (ex.: `https://focodev.com.br`) |
| `CLOUDFLARED_TUNNEL_TOKEN` | Token do conector Cloudflare (produção) |
| `ASAAS_WEBHOOK_TOKEN` | Token de autenticação do webhook Asaas |

> O `docker-compose.yml` **sobrescreve** `DB_URL`, `PORT` e credenciais do banco para os hostnames internos do Docker. Valores como `localhost` no `.env` são normais para desenvolvimento local fora do container.

### 3. Configurar segredo Asaas

```powershell
copy docker\secrets\asaas_sandbox_api_key.txt.example docker\secrets\asaas_sandbox_api_key.txt
```

Edite `docker/secrets/asaas_sandbox_api_key.txt` e cole **uma linha** com a chave sandbox (`$aact_hmlg_...`), sem aspas.

> Esse arquivo está no `.gitignore` — **nunca** commite chaves reais.

### 4. (Produção) Instalar chave SSH no Hetzner

Execute **uma vez** na sua máquina:

```powershell
.\scripts\instalar-chave-ssh.ps1
```

Teste:

```powershell
ssh -o BatchMode=yes hetzner "echo OK"
```

---

## Subir os containers (local)

Na raiz do projeto:

```powershell
docker compose up -d --build
```

### O que acontece, em ordem

1. **postgres** — sobe e aguarda healthcheck (`pg_isready`)
2. **app** — build Maven (~2–5 min na primeira vez), inicia Spring Boot, Flyway roda migrations
3. **nginx** — build Angular (~2–4 min), sobe na porta `${HTTP_PORT:-80}`
4. **cloudflared** — conecta ao túnel Cloudflare (precisa de token válido)

### Acompanhar logs

```powershell
# Todos os serviços
docker compose logs -f

# Apenas backend
docker compose logs -f app

# Apenas nginx / frontend
docker compose logs -f nginx
```

### Verificar status

```powershell
docker compose ps
```

Todos devem estar `Up` e `healthy` (app e postgres têm healthcheck).

### Acessar localmente

| O quê | URL |
|-------|-----|
| Frontend | http://localhost |
| API (via nginx) | http://localhost/auth/login |
| Health backend | http://localhost/actuator/health |

---

## Parar, reiniciar e limpar

```powershell
# Parar containers (mantém volumes/dados)
docker compose down

# Parar e remover volumes (APAGA o banco PostgreSQL)
docker compose down -v

# Reiniciar um serviço específico
docker compose restart app

# Rebuild forçado após mudanças no código
docker compose up -d --build
```

---

## Deploy em produção (Hetzner)

Produção usa o servidor **ubuntu-4gb-fsn1-1** (`178.105.11.19`), alias SSH `hetzner`.  
Diretório no servidor: `/opt/gerenciamento-estoque`.

### Processo completo (da sua máquina)

#### Passo 1 — Gerar pacote de deploy

Na raiz do repositório (PowerShell):

```powershell
Set-Location c:\Gerenciamento_de_estoque

tar --exclude="node_modules" `
    --exclude="Gerenciamento_de_estoque_front/node_modules" `
    --exclude="Gerenciamento_de_estoque_front/dist" `
    --exclude="Gerenciamento_de_estoque_front/.angular" `
    --exclude="Gerenciamento_de_estoque_front_backup" `
    --exclude="target" `
    --exclude=".git" `
    --exclude=".env" `
    --exclude="jdk-21_windows-x64_bin" `
    --exclude="gerenciamento-deploy.tgz" `
    -czf gerenciamento-deploy.tgz .
```

> **Importante:** o `.env` do servidor **não** entra no pacote — as credenciais de produção ficam só no servidor.

#### Passo 2 — Enviar para o servidor

```powershell
scp gerenciamento-deploy.tgz hetzner:/root/gerenciamento-deploy.tgz
```

#### Passo 3 — Extrair e rebuild no servidor

```powershell
ssh hetzner "cd /opt/gerenciamento-estoque && tar xzf /root/gerenciamento-deploy.tgz && docker compose up -d --build 2>&1 | tee -a /root/deploy.log"
```

O rebuild leva cerca de **3–5 minutos** (Maven + Angular).

#### Passo 4 — Validar deploy

```powershell
ssh hetzner "docker compose -f /opt/gerenciamento-estoque/docker-compose.yml ps"
```

```powershell
ssh hetzner "docker logs gerenciamento-app 2>&1 | grep -i flyway | tail -5"
```

Confirme que as migrations foram aplicadas (ex.: `Successfully applied ... now at version vXX`).

Teste HTTP no servidor:

```powershell
ssh hetzner "curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/categorias"
```

Resposta esperada: `401` (rota existe, exige login) — **não** `404` ou `500`.

#### Passo 5 — Limpar cache do navegador

Após deploy do frontend, faça **hard refresh** (Ctrl+Shift+R) em https://focodev.com.br para carregar os novos arquivos JS.

---

## Configuração do `.env` em produção

No servidor, edite `/opt/gerenciamento-estoque/.env`:

```bash
ssh hetzner
nano /opt/gerenciamento-estoque/.env
```

Valores típicos de produção:

```env
APP_PUBLIC_URL=https://focodev.com.br
HTTP_PORT=80
CLOUDFLARED_TUNNEL_TOKEN=<token-do-painel-cloudflare>
JWT_SECRET=<segredo-forte>
APP_PAYMENT_PROVIDER=asaas
ASAAS_ENVIRONMENT=sandbox
ASAAS_WEBHOOK_TOKEN=<token-webhook>
```

Após alterar `.env`:

```bash
cd /opt/gerenciamento-estoque
docker compose up -d
```

---

## Acesso SSH ao servidor

| Campo | Valor |
|-------|-------|
| Alias | `hetzner` ou `ubuntu-4gb-fsn1-1` |
| IP | `178.105.11.19` |
| Usuário | `root` |
| Auth | Chave SSH (`~/.ssh/id_rsa`) |

Comandos remotos sem abrir sessão interativa:

```powershell
ssh hetzner "docker ps"
ssh hetzner "docker logs gerenciamento-app --tail 100"
```

---

## Comandos úteis no dia a dia

### Logs e diagnóstico

```powershell
# Últimas 200 linhas do backend
ssh hetzner "docker logs gerenciamento-app --tail 200"

# Erros Flyway / startup
ssh hetzner "docker logs gerenciamento-app 2>&1 | grep -iE 'error|flyway|exception' | tail -30"

# Logs do túnel Cloudflare
ssh hetzner "docker logs gerenciamento-cloudflared --tail 50"
```

### Banco de dados

```powershell
# Entrar no psql dentro do container
ssh hetzner "docker exec -it gerenciamento-postgres psql -U postgres -d gerenciamento_estoque"
```

Consultas úteis:

```sql
-- Versão das migrations Flyway
SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- Categorias por organização
SELECT id, nome, org_id FROM categorias LIMIT 20;
```

### Rebuild apenas um serviço

```powershell
# Só backend (mudança Java)
ssh hetzner "cd /opt/gerenciamento-estoque && docker compose up -d --build app"

# Só frontend/nginx (mudança Angular)
ssh hetzner "cd /opt/gerenciamento-estoque && docker compose up -d --build nginx"
```

---

## Desenvolvimento sem Docker (opcional)

Se preferir rodar backend e frontend separadamente na máquina local:

### Backend

```powershell
# PostgreSQL precisa estar acessível em localhost:5432
.\mvnw spring-boot:run
```

Porta padrão no `.env` local: `8082`.

### Frontend

```powershell
cd Gerenciamento_de_estoque_front
npm install
npm start
```

`environment.ts` aponta para `http://localhost:8081` (ajuste conforme sua porta do backend).

> Em produção, **sempre** use o build Docker (`configuration=docker`) para que a API seja servida pelo nginx no mesmo domínio.

---

## Troubleshooting

### Container `app` não fica healthy

```powershell
docker logs gerenciamento-app --tail 100
```

Causas comuns:
- Migration Flyway falhou (conflito de schema)
- `.env` com variável obrigatória ausente
- Arquivo `docker/secrets/asaas_sandbox_api_key.txt` inexistente ou vazio

### Erro 502 / nginx não responde

```powershell
docker compose ps
docker logs gerenciamento-nginx --tail 50
```

Verifique se `gerenciamento-app` está `healthy` antes do nginx subir.

### Frontend chama URL errada da API

- Build Docker usa `environment.docker.ts` (`apiUrl: ''`)
- Build produção standalone usa `environment.prod.ts` (`apiUrl: 'https://focodev.com.br'`)
- Após deploy, limpe cache do navegador

### Rota da API retorna 404 pelo nginx

O nginx só faz proxy para prefixos listados em `docker/nginx/conf.d/default.conf`.  
Se criar um novo módulo (ex.: `/fornecedores`), adicione o prefixo na regex `location` e rebuild o nginx:

```powershell
docker compose up -d --build nginx
```

### Migration falhou no deploy

```powershell
ssh hetzner "docker logs gerenciamento-app 2>&1 | grep -i flyway"
```

Corrija o SQL em `src/main/resources/db/migration/`, faça novo deploy.  
**Nunca** edite migrations já aplicadas em produção — crie sempre uma nova `VXX__...sql`.

### Túnel Cloudflare offline

```powershell
ssh hetzner "docker logs gerenciamento-cloudflared --tail 30"
```

Verifique `CLOUDFLARED_TUNNEL_TOKEN` no `.env` do servidor.

---

## Checklist rápido de deploy

- [ ] Código testado localmente
- [ ] Nova migration criada (se houver mudança no banco)
- [ ] Pacote `.tgz` gerado (sem `.env` e sem `node_modules`)
- [ ] Upload via `scp` para o servidor
- [ ] `tar xzf` + `docker compose up -d --build`
- [ ] Containers `healthy` em `docker compose ps`
- [ ] Flyway aplicou migrations (`docker logs gerenciamento-app`)
- [ ] Hard refresh no navegador
- [ ] Teste funcional (login, criar categoria, etc.)

---

## Referência de arquivos

| Arquivo | Papel |
|---------|-------|
| `docker-compose.yml` | Orquestração dos 4 serviços |
| `Dockerfile` | Build do backend Spring Boot |
| `docker/nginx/Dockerfile` | Build Angular + imagem Nginx |
| `docker/nginx/conf.d/default.conf` | Rotas proxy API vs SPA |
| `.env.example` | Modelo de variáveis de ambiente |
| `docker/secrets/asaas_sandbox_api_key.txt` | Chave Asaas (não versionada) |
| `Gerenciamento_de_estoque_front/src/environments/environment.docker.ts` | API URL vazia (same-origin) |
| `src/main/resources/db/migration/V*.sql` | Migrations Flyway |
