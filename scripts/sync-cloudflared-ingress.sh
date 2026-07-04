#!/usr/bin/env bash
# Gera config.yml + credentials.json do tunel a partir de CLOUDFLARED_TUNNEL_TOKEN no .env
# e reinicia cloudflared com ingress local (sem depender do painel remoto).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT/.env}"
CF_DIR="${CF_DIR:-$ROOT/docker/cloudflared}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERRO: $ENV_FILE nao encontrado."
  exit 1
fi

mkdir -p "$CF_DIR"

python3 - "$ENV_FILE" "$CF_DIR" <<'PY'
import json, base64, os, sys

env_path, cf_dir = sys.argv[1:3]
token = None
for line in open(env_path):
    if line.startswith("CLOUDFLARED_TUNNEL_TOKEN="):
        token = line.split("=", 1)[1].strip()
        break
if not token:
    raise SystemExit("CLOUDFLARED_TUNNEL_TOKEN ausente no .env")

raw = token + "=" * (-len(token) % 4)
data = json.loads(base64.urlsafe_b64decode(raw))
tunnel_id = data["t"]

cred = {
    "AccountTag": data["a"],
    "TunnelSecret": data["s"],
    "TunnelID": tunnel_id,
}
cred_path = os.path.join(cf_dir, "credentials.json")
with open(cred_path, "w") as f:
    json.dump(cred, f)
os.chmod(cred_path, 0o644)

config = f"""tunnel: {tunnel_id}
credentials-file: /etc/cloudflared/credentials.json

ingress:
  - hostname: focodev.com.br
    service: http://localhost:80
  - hostname: www.focodev.com.br
    service: http://127.0.0.1:8082
  - hostname: painel.focodev.com.br
    service: http://127.0.0.1:8082
  - hostname: rotas.focodev.com.br
    service: http://127.0.0.1:8081
  - hostname: admin.rotas.focodev.com.br
    service: http://127.0.0.1:8081
  - hostname: admin-rotas.focodev.com.br
    service: http://127.0.0.1:8081
  - hostname: portal-rotas.focodev.com.br
    service: http://127.0.0.1:8081
  - hostname: mercado.focodev.com.br
    service: http://127.0.0.1:8083
  - hostname: admin.mercado.focodev.com.br
    service: http://127.0.0.1:8083
  - service: http_status:404
"""
config_path = os.path.join(cf_dir, "config.yml")
with open(config_path, "w") as f:
    f.write(config)
os.chmod(config_path, 0o644)
print(f"OK: {config_path}")
print(f"OK: {cred_path}")
PY

cd "$ROOT"
docker compose up -d cloudflared
sleep 3
docker ps --filter name=gerenciamento-cloudflared --format '{{.Names}} {{.Status}}'
echo "Verifique:"
echo "  curl -sI https://focodev.com.br/        # Gerenciamento de Estoque"
echo "  curl -sI https://www.focodev.com.br/     # Site institucional"
