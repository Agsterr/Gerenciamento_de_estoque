# AGENTS.md — Gerenciamento de Estoque

Instruções para agentes de IA (Cursor Cloud, CLI e IDE) que trabalham neste repositório.

## Repositório

| Item | Valor |
|------|--------|
| Repo GitHub | `Agster/Gerenciamento_de_estoque` |
| Produção | https://focodev.com.br |
| Servidor | Hetzner — `/opt/gerenciamento-estoque` |
| Stack | Spring Boot 3 + PostgreSQL + Angular + Nginx + Cloudflare Tunnel |

### Estrutura

- `src/main/java/` — backend Spring Boot
- `Gerenciamento_de_estoque_front/` — frontend Angular (build Docker usa `environment.docker.ts` com `apiUrl: ''`)
- `docker-compose.yml` — postgres, app, nginx, cloudflared
- `docker/nginx/conf.d/default.conf` — proxy API + SPA + redirect HTTPS

Não confundir com `Gerenciamento_de_estoque_front` na raiz de `src/` (legado) — o build oficial é `Gerenciamento_de_estoque_front/` na raiz.

---

## Segredos — NUNCA commitar

- `.env` (local e produção)
- `docker/secrets/*.txt` (chaves Asaas)
- `JWT_SECRET`, tokens Mercado Pago/Asaas, `CLOUDFLARED_TUNNEL_TOKEN`

Usar `.env.example` apenas como modelo.

---

## Variáveis críticas

### `APP_PUBLIC_URL`

- **Local/Docker dev:** `http://localhost`
- **Produção (Hetzner):** `https://focodev.com.br` (sempre **HTTPS**)

Usada para webhooks Asaas, URLs de retorno de pagamento e links públicos. URL HTTP em produção causa aviso "site não seguro" e callbacks errados.

O `docker-compose.yml` sobrescreve `DB_URL` para o hostname `postgres`; o `.env` local pode ter `localhost` — isso é intencional.

### Cache

- Produção Docker: `CACHE_TYPE=simple`, `SPRING_DATA_REDIS_ENABLED=false`
- Redis desligado → `SimpleCacheConfig` fornece `CacheManager` em memória
- Admin → Cache (`/api/cache/*`) exige `ROLE_SUPER_ADMIN`

### CORS

Origens de produção já em `SecurityConfig.java`:

```text
https://focodev.com.br
https://www.focodev.com.br
```

Ao adicionar domínio novo, incluir em `setAllowedOrigins`.

---

## Subir ambiente local (Docker)

Ordem natural do Compose:

1. **postgres** — aguardar `healthy` (`pg_isready`)
2. **app** — Spring na porta 8080 (interna), healthcheck em `/api/asaas/config`
3. **nginx** — porta 80 no host (`HTTP_PORT`)
4. **cloudflared** — opcional local; produção usa token fixo

```powershell
docker compose up -d --build
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Aguardar `gerenciamento-app` e `gerenciamento-postgres` **healthy** (~1–2 min).

Smoke test:

```powershell
.\scripts\test-api.ps1 -BaseUrl "http://localhost"
```

Build manual:

```powershell
.\mvnw.cmd -q "-Dmaven.test.skip=true" package
cd Gerenciamento_de_estoque_front; npm run build; cd ..
```

---

## HTTPS / Nginx

- Cloudflare termina TLS; nginx escuta só porta 80
- Redirect HTTP→HTTPS **somente** quando `X-Forwarded-Proto: http` (Cloudflare)
- **Sem** esse header (localhost) → não redireciona (dev local continua em HTTP)
- `server.forward-headers-strategy=native` no Spring para reconhecer HTTPS atrás do proxy

---

## Deploy produção (Hetzner)

Path no servidor: `/opt/gerenciamento-estoque`

```powershell
$archive = "$env:TEMP\gerenciamento-deploy.tgz"
tar -czf $archive `
  --exclude=node_modules --exclude=target --exclude=.git `
  --exclude=Gerenciamento_de_estoque_front/node_modules `
  --exclude=Gerenciamento_de_estoque_front/dist `
  --exclude=Gerenciamento_de_estoque_front/.angular `
  --exclude=.cursor .

scp $archive hetzner:/root/gerenciamento-deploy.tgz
ssh hetzner "mkdir -p /opt/gerenciamento-estoque && cd /opt/gerenciamento-estoque && tar xzf /root/gerenciamento-deploy.tgz"
ssh hetzner "cd /opt/gerenciamento-estoque && docker compose up -d --build"
```

Smoke test produção:

```powershell
.\scripts\test-api.ps1 -BaseUrl "https://focodev.com.br"
```

Webhook Asaas: `https://focodev.com.br/api/webhooks/asaas`

---

## Gotchas conhecidos

### LazyInitializationException em listagens

DTOs que acessam coleções lazy (`itens` em pedidos/contagens) **devem** ser mapeados dentro de `@Transactional(readOnly = true)` no service (`listarDto`, `buscarDto`), não no controller após fechar a sessão.

Afeta: `/pedidos-venda`, `/pedidos-compra`, `/inventario/contagens`.

### Inventário (frontend)

A tela **não** lista produtos como "contagens". Mostra:

1. **Estoque atual** — produtos cadastrados
2. **Contagens** — sessões de conferência física

Para iniciar contagem: cadastrar **Depósito** → **Nova Contagem**. Sem depósito, o fluxo bloqueia.

### Admin Cache retornava 404

Ocorria quando não havia `CacheManager` (Redis off e sem `SimpleCacheConfig`). Corrigido — endpoint retorna 200 com info dos caches em memória.

### Assinatura / endpoints admin

- `/api/cache/**` → `SUPER_ADMIN`
- `/admin/**` → `SUPER_ADMIN`
- `/api/subscription/status` pode retornar 500 em contas de teste — não bloqueia login

---

## Após alterar código

1. Build backend + frontend
2. Testar com Docker local
3. Deploy Hetzner (se pedido ou mudança de runtime/nginx/Docker)
4. Smoke test em https://focodev.com.br

Não commitar `.env` ou secrets. Só criar commit quando o usuário pedir explicitamente.

---

## Login de teste (smoke)

Credenciais padrão do script `scripts/test-api.ps1`:

- Usuário: `admin`
- Senha: `admin123`

Usar apenas em ambientes de dev/teste conhecidos.
