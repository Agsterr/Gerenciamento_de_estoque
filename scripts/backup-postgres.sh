#!/usr/bin/env bash
# Backup periódico do PostgreSQL (Gerenciamento de Estoque)
# Uso: ./scripts/backup-postgres.sh
# Cron exemplo (Hetzner, 03:00 diário):
#   0 3 * * * /opt/gerenciamento-estoque/scripts/backup-postgres.sh >> /var/log/gerenciamento-backup.log 2>&1

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-gerenciamento_estoque}"
DB_USER="${DB_USER:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-${ROOT_DIR}/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

mkdir -p "${BACKUP_DIR}"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

echo "[$(date -Iseconds)] Iniciando backup -> ${FILE}"

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -qx 'gerenciamento-postgres'; then
  docker exec -e PGPASSWORD="${DB_PASSWORD:-postgres}" gerenciamento-postgres \
    pg_dump -U "${DB_USER}" -d "${DB_NAME}" --no-owner --no-acl | gzip > "${FILE}"
elif command -v pg_dump >/dev/null 2>&1; then
  PGPASSWORD="${DB_PASSWORD:-postgres}" pg_dump -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
    --no-owner --no-acl | gzip > "${FILE}"
else
  echo "Erro: pg_dump não encontrado e container gerenciamento-postgres indisponível." >&2
  exit 1
fi

find "${BACKUP_DIR}" -name "${DB_NAME}_*.sql.gz" -type f -mtime +"${RETENTION_DAYS}" -delete

echo "[$(date -Iseconds)] Backup concluído ($(du -h "${FILE}" | cut -f1))"
