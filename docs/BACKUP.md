# Backup do banco PostgreSQL

Backup periódico dos dados críticos do Gerenciamento de Estoque.

## Script

```bash
./scripts/backup-postgres.sh
```

Variáveis opcionais (sem commitar valores reais):

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DB_HOST` | `postgres` | Host PostgreSQL |
| `DB_PORT` | `5432` | Porta |
| `DB_NAME` | `gerenciamento_estoque` | Nome do banco |
| `DB_USER` | `postgres` | Usuário |
| `DB_PASSWORD` | `postgres` | Senha (usar env, não commitar) |
| `BACKUP_DIR` | `./backups` | Pasta de destino |
| `BACKUP_RETENTION_DAYS` | `14` | Dias para manter arquivos |

Arquivos gerados: `backups/gerenciamento_estoque_YYYYMMDD_HHMMSS.sql.gz`

A pasta `backups/` está no `.gitignore` — não versionar dumps.

## Docker Compose (serviço opcional)

Ative o profile `backup` para um sidecar com cron diário às 03:00:

```bash
docker compose --profile backup up -d db-backup
```

Configure no `.env`:

```env
BACKUP_RETENTION_DAYS=14
```

## Cron no servidor Hetzner (recomendado em produção)

```bash
chmod +x /opt/gerenciamento-estoque/scripts/backup-postgres.sh
crontab -e
```

Entrada sugerida:

```cron
0 3 * * * DB_PASSWORD='***' /opt/gerenciamento-estoque/scripts/backup-postgres.sh >> /var/log/gerenciamento-backup.log 2>&1
```

Substitua `DB_PASSWORD` pela senha real do `.env` do servidor (não commitar).

## Restauração

```bash
gunzip -c backups/gerenciamento_estoque_YYYYMMDD_HHMMSS.sql.gz | \
  docker exec -i gerenciamento-postgres psql -U postgres -d gerenciamento_estoque
```

Teste restauração em ambiente de staging antes de usar em produção.

## Arquivo de logs de login

Logs de acesso (`acessos_login`) podem ser exportados para arquivos `.json.gz` compactados.

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `LOGIN_LOG_EXPORT_DIR` | `data/log-exports` | Pasta de destino (gitignored) |
| `LOGIN_LOG_AUTO_ARCHIVE` | `false` | Job diário exporta + apaga logs antigos |
| `LOGIN_LOG_AUTO_ARCHIVE_RETENTION_DAYS` | `90` | Logs anteriores a N dias são compactados |
| `LOGIN_LOG_AUTO_ARCHIVE_CRON` | `0 0 4 * * *` | Horário do job (04:00) |

Arquivos gerados: `login-logs_org{id}_{periodo}_{timestamp}.json.gz` ou `login-logs_global_{periodo}_{timestamp}.json.gz`

No Docker Compose, a pasta `./data/log-exports` é montada em `/app/data/log-exports`.

### Endpoints (SUPER_ADMIN)

- `GET /admin/login-logs/export?ano=&mes=&dia=&orgId=` — exporta e baixa
- `POST /admin/login-logs/compact` — exporta + apaga do banco
- `POST /admin/login-logs/delete` — apaga do banco (`confirm: true`)
- `GET /admin/login-logs/arquivos` — lista arquivos no disco
- `GET /admin/login-logs/arquivos/{filename}` — baixa arquivo existente
- `DELETE /admin/login-logs/arquivos/{filename}` — remove arquivo do disco

Org ADMIN (`/api/org/login-logs/*`) tem as mesmas ações restritas à própria organização.

### Restaurar logs exportados

```bash
gunzip -c data/log-exports/login-logs_global_2024-06-15_*.json.gz | jq .
```

Os arquivos são JSON (array de registros). Reimportação ao banco não é automática — use apenas para auditoria offline.
