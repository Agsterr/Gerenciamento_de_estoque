# CI/CD — Deploy automático no Hetzner

Pipeline: **push em `main`** → GitHub Actions → SSH no Hetzner → `git pull` + `docker compose up -d --build`.

| Item | Valor |
|------|--------|
| Repositório | https://github.com/Agsterr/Gerenciamento_de_estoque |
| Branch de deploy | `main` |
| Servidor | `178.105.11.19` (`ssh hetzner`) |
| Path | `/opt/gerenciamento-estoque` |
| Domínio | https://focodev.com.br |

## 1. Deploy key no servidor (git pull)

No Hetzner, após o primeiro push destes scripts:

```bash
ssh hetzner
bash /opt/gerenciamento-estoque/scripts/setup-git-hetzner.sh
```

O script exibe a chave pública. Adicione em **GitHub → Settings → Deploy keys → Add deploy key** (marque **Allow read access only**).

Rode o script de novo até aparecer `successfully authenticated`.

## 2. Secrets no GitHub (Actions → SSH no servidor)

Em **Settings → Secrets and variables → Actions**, crie:

| Secret | Valor |
|--------|--------|
| `HETZNER_HOST` | `178.105.11.19` |
| `HETZNER_USER` | `root` |
| `HETZNER_SSH_KEY` | Chave privada SSH com acesso ao servidor |

### Opção A — Chave dedicada para CI (recomendado)

No seu PC:

```powershell
ssh-keygen -t ed25519 -f $env:USERPROFILE\.ssh\hetzner_github_actions -N '""'
type $env:USERPROFILE\.ssh\hetzner_github_actions.pub | ssh hetzner "cat >> ~/.ssh/authorized_keys"
```

Copie o conteúdo de `hetzner_github_actions` (privada) para o secret `HETZNER_SSH_KEY`.

### Opção B — Reutilizar chave existente

Se `ssh hetzner` já funciona, use a chave privada correspondente (ex.: `~/.ssh/id_rsa`) em `HETZNER_SSH_KEY`. Menos isolamento, mas funciona.

**Nunca** commite chaves privadas ou `.env`.

## 3. Fluxo após configurar

1. Altere código localmente
2. `git commit` + `git push origin main`
3. GitHub Actions executa `.github/workflows/deploy-hetzner.yml`
4. Servidor roda `scripts/deploy-hetzner.sh` (pull + rebuild Docker)

Acompanhe em **Actions** no GitHub.

## 4. Agentes Cursor (cloud / mobile)

Os agentes na nuvem **não têm** seu `ssh hetzner` local. Duas formas de deploy:

| Método | Como |
|--------|------|
| **Recomendado** | Agente faz commit + push → GitHub Actions faz o deploy |
| **Direto SSH** | Configurar secret `HETZNER_SSH_KEY` no ambiente Cursor (se disponível) e rodar `ssh` + `deploy-hetzner.sh` |

Para uso mobile: peça ao agente *"commit e push para main"* — o deploy dispara sozinho após os secrets estarem configurados.

## 5. Deploy manual (fallback)

```bash
ssh hetzner "bash /opt/gerenciamento-estoque/scripts/deploy-hetzner.sh"
```

## 6. Primeira vez com código ainda não no GitHub

1. Commit e push destes arquivos (`workflow`, `scripts/`, `docs/CICD.md`)
2. Envie os scripts ao servidor (ou rode setup após o primeiro deploy manual via tarball)
3. Configure deploy key + secrets
4. Próximos pushes em `main` deployam automaticamente

## 7. O que não vai no Git

- `.env` de produção
- `docker/secrets/*.txt`
- Chaves SSH privadas

Esses arquivos permanecem só no servidor.
