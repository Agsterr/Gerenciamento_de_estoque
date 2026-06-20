---
name: alterar-testar-deploy
description: >-
  Garante que alterações de código sejam buildadas e testadas localmente antes
  de qualquer deploy. Use após implementar, fix, feature ou refactor; quando o
  usuário pedir testar, docker, deploy, hetzner ou concluir uma feature.
---

# Alterar → Testar → (perguntar) → Deploy

Fluxo obrigatório após **qualquer alteração de código**. Referências: `AGENTS.md`, `docs/CICD.md`.

Skills relacionadas: `testar-api-gerenciamento-estoque`, `deploy-producao-hetzner`, `cicd-hetzner-github-actions`.

## Regra de ouro

**Nunca fazer deploy automaticamente.** Testes locais OK ≠ deploy. Sempre perguntar ao usuário antes de commit, push ou deploy.

## Checklist

```
- [ ] 1. Build backend (se Java/backend alterado)
- [ ] 2. Build frontend (se Angular alterado)
- [ ] 3. Docker Compose local (quando relevante)
- [ ] 4. Smoke test local
- [ ] 5. Reportar resultado (pass/fail)
- [ ] 6. Perguntar ao usuário sobre deploy
- [ ] 7. Commit/push/deploy SOMENTE se confirmado
```

## 1. Build backend

Quando `src/`, `pom.xml`, migrations Flyway ou Docker backend mudaram:

```powershell
.\mvnw.cmd -q "-Dmaven.test.skip=true" package
```

Falhou → corrigir e repetir. Não avançar.

Opcional (mudança crítica): `.\mvnw.cmd test` ou teste de controller específico.

## 2. Build frontend

Quando `Gerenciamento_de_estoque_front/` mudou:

```powershell
cd Gerenciamento_de_estoque_front; npm run build; cd ..
```

Build oficial usa `environment.docker.ts` (`apiUrl: ''`).

## 3. Docker local

Quando backend, frontend, nginx, `docker-compose.yml` ou migrations mudaram:

```powershell
docker info 2>$null
# Se falhar: iniciar Docker Desktop e aguardar daemon (~2 min)

docker compose up -d --build
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Aguardar `gerenciamento-app` e `gerenciamento-postgres` **healthy** (~1–2 min).

Sem Docker Desktop → informar; fazer builds Maven/npm; **não** deploy remoto sem avisar.

## 4. Smoke test local

```powershell
.\scripts\test-api.ps1 -BaseUrl "http://localhost"
```

Mínimo se endpoint específico mudou: health + login + endpoint afetado. Detalhes: skill `testar-api-gerenciamento-estoque`.

## 5. Reportar resultado

Informar claramente:

| Item | Status |
|------|--------|
| Build backend | OK / falhou / omitido |
| Build frontend | OK / falhou / omitido |
| Docker local | OK / falhou / omitido |
| Smoke test | **PASSOU** / **FALHOU** |

Se falhou → corrigir. Não perguntar deploy.

## 6. Perguntar ao usuário (obrigatório se testes passaram)

Usar esta pergunta (ou equivalente):

> **Testes locais passaram. Quer fazer commit/push para deploy agora ou ainda tem mais alterações?**

Aguardar resposta. Opções típicas:

- **Mais alterações** → parar; não commitar nem fazer push.
- **Pronto para deploy** → seguir passo 7.

## 7. Commit, push e deploy (só com confirmação)

### Commit

- **Somente** quando o usuário pedir explicitamente (regra do projeto).
- Nunca commitar `.env`, `docker/secrets/`, logs ou artefatos (`target/`, `dist/`).

### Deploy preferido — CI/CD

Push em `main` dispara GitHub Actions → Hetzner:

```powershell
git push origin main
```

Monitorar: https://github.com/Agsterr/Gerenciamento_de_estoque/actions

Detalhes: `docs/CICD.md`, skill `cicd-hetzner-github-actions`.

### Fallback — SSH manual (IDE local)

```powershell
ssh hetzner "bash /opt/gerenciamento-estoque/scripts/deploy-hetzner.sh"
```

Ou tarball: ver `deploy-producao-hetzner`. Preferir push quando CI/CD ativo.

### Smoke test produção (após deploy confirmado)

```powershell
.\scripts\test-api.ps1 -BaseUrl "https://focodev.com.br"
```

## Quando pular etapas

| Situação | Ação |
|----------|------|
| Usuário disse "só código", "sem deploy" ou "sem testar" | Omitir passos indicados; informar o que foi pulado |
| Só documentação (`.md`) | Sem build/docker |
| Pergunta ou review sem editar código | Skill não se aplica |

## Quando teste completo é obrigatório

Backend, frontend, Flyway, nginx, Dockerfile, `docker-compose.yml`, CORS ou `.env` de runtime.

## Regras para o agente

1. **Executar** builds e testes — não só descrever.
2. **Nunca** commit, push ou deploy sem confirmação explícita do usuário.
3. Testes OK → **perguntar**; não assumir que o usuário quer publicar.
4. Corrigir falhas antes de qualquer deploy.
5. Nunca expor segredos em chat ou commits.
6. Agentes cloud sem SSH local: commit + push na `main` é o caminho para produção.
