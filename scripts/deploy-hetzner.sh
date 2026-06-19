#!/usr/bin/env bash
# Deploy no servidor Hetzner: git pull + docker compose rebuild.
# Chamado pelo GitHub Actions ou manualmente: bash scripts/deploy-hetzner.sh
set -euo pipefail

APP_DIR="/opt/gerenciamento-estoque"
BRANCH="${DEPLOY_BRANCH:-main}"
DEPLOY_KEY="/root/.ssh/github_deploy"

cd "$APP_DIR"

if [[ ! -d .git ]]; then
  echo "ERRO: ${APP_DIR} nao e um repositorio git."
  echo "Execute uma vez: bash ${APP_DIR}/scripts/setup-git-hetzner.sh"
  exit 1
fi

if [[ -f "$DEPLOY_KEY" ]]; then
  export GIT_SSH_COMMAND="ssh -i ${DEPLOY_KEY} -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new"
fi

echo "==> Atualizando codigo (origin/${BRANCH})"
git fetch origin "$BRANCH"
git reset --hard "origin/${BRANCH}"

echo "==> Rebuild e restart (docker compose)"
docker compose up -d --build

echo "==> Status dos containers"
docker ps --format 'table {{.Names}}\t{{.Status}}'

echo "Deploy concluido em $(date -u +'%Y-%m-%dT%H:%M:%SZ')."