#!/bin/sh
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
DB_HOST="${DB_HOST:-postgres}"
DB_NAME="${DB_NAME:-gerenciamento_estoque}"
DB_USER="${DB_USER:-postgres}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

mkdir -p "${BACKUP_DIR}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

echo "[$(date -Iseconds)] Backup cron -> ${FILE}"

PGPASSWORD="${DB_PASSWORD:-postgres}" pg_dump -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" \
  --no-owner --no-acl | gzip > "${FILE}"

find "${BACKUP_DIR}" -name "${DB_NAME}_*.sql.gz" -type f -mtime +"${RETENTION_DAYS}" -delete

echo "[$(date -Iseconds)] Concluído"
