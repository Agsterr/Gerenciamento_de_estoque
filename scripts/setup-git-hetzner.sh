#!/usr/bin/env bash
# Configuracao unica no Hetzner: deploy key GitHub + git clone in-place.
# Preserva .env e docker/secrets/ (nao versionados).
set -euo pipefail

APP_DIR="/opt/gerenciamento-estoque"
REPO="git@github.com:Agsterr/Gerenciamento_de_estoque.git"
DEPLOY_KEY="/root/.ssh/github_deploy"
BRANCH="${DEPLOY_BRANCH:-main}"

mkdir -p /root/.ssh
chmod 700 /root/.ssh

if ! grep -q "^github.com" /root/.ssh/known_hosts 2>/dev/null; then
  ssh-keyscan -H github.com >> /root/.ssh/known_hosts 2>/dev/null
fi

if [[ ! -f "$DEPLOY_KEY" ]]; then
  echo "==> Gerando deploy key para GitHub (read-only)"
  ssh-keygen -t ed25519 -f "$DEPLOY_KEY" -N "" -C "hetzner-deploy@focodev.com.br"
fi

if [[ ! -f /root/.ssh/config ]] || ! grep -q "Host github.com" /root/.ssh/config; then
  cat >> /root/.ssh/config <<EOF

Host github.com
  HostName github.com
  User git
  IdentityFile ${DEPLOY_KEY}
  IdentitiesOnly yes
EOF
  chmod 600 /root/.ssh/config
fi

echo ""
echo "=========================================="
echo " ADICIONE ESTA CHAVE NO GITHUB (read-only)"
echo " https://github.com/Agsterr/Gerenciamento_de_estoque/settings/keys"
echo "=========================================="
cat "${DEPLOY_KEY}.pub"
echo "=========================================="
echo ""

auth_msg=$(ssh -T -o BatchMode=yes -o ConnectTimeout=10 git@github.com 2>&1) || true
if ! printf '%s\n' "$auth_msg" | grep -qi "successfully authenticated"; then
  echo "AVISO: GitHub ainda nao aceita a deploy key. Adicione a chave acima e rode este script de novo."
  exit 1
fi

mkdir -p "$APP_DIR"
cd "$APP_DIR"

if [[ ! -d .git ]]; then
  echo "==> Inicializando repositorio git em ${APP_DIR}"
  git init
  git remote add origin "$REPO" 2>/dev/null || git remote set-url origin "$REPO"
fi

export GIT_SSH_COMMAND="ssh -i ${DEPLOY_KEY} -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new"

backup_preserved_files() {
  local backup_dir
  backup_dir="/root/setup-git-backup-$(date +%Y%m%d%H%M%S)"
  mkdir -p "$backup_dir"
  [[ -f .env ]] && cp -a .env "$backup_dir/.env"
  [[ -d docker/secrets ]] && cp -a docker/secrets "$backup_dir/docker-secrets"
  echo "$backup_dir"
}

restore_preserved_files() {
  local backup_dir="$1"
  [[ -f "$backup_dir/.env" && ! -f .env ]] && cp -a "$backup_dir/.env" .env
  if [[ -d "$backup_dir/docker-secrets" && ! -d docker/secrets ]]; then
    mkdir -p docker
    cp -a "$backup_dir/docker-secrets" docker/secrets
  fi
}

git_clean_preserve_local() {
  # Remove lixo de tarball/deploy sem apagar segredos nem caches pesados.
  git clean -fd \
    -e .env \
    -e docker/secrets \
    -e node_modules \
    -e dist \
    -e .angular \
    -e scripts
}

sync_with_origin() {
  local backup_dir
  backup_dir=$(backup_preserved_files)

  echo "==> Sincronizando com origin/${BRANCH}"
  git fetch origin "$BRANCH"

  # Migracao in-place: checkout pode falhar com untracked do tarball; reset --hard resolve.
  git checkout -B "$BRANCH" "origin/${BRANCH}" 2>/dev/null || true
  git reset --hard "origin/${BRANCH}"
  git branch -M "$BRANCH" 2>/dev/null || true

  git_clean_preserve_local
  restore_preserved_files "$backup_dir"

  local head remote
  head=$(git rev-parse HEAD)
  remote=$(git rev-parse "origin/${BRANCH}")
  if [[ "$head" != "$remote" ]]; then
    echo "ERRO: HEAD ($head) diverge de origin/${BRANCH} ($remote)"
    exit 1
  fi

  if [[ -n "$(git status --porcelain)" ]]; then
    echo "AVISO: working tree ainda tem alteracoes locais (veja git status):"
    git status -sb
  else
    echo "Repositorio alinhado com origin/${BRANCH} ($(git rev-parse --short HEAD))"
  fi
}

sync_with_origin

chmod +x "${APP_DIR}/scripts/deploy-hetzner.sh" 2>/dev/null || true

echo "Setup git concluido. Deploy manual: bash ${APP_DIR}/scripts/deploy-hetzner.sh"