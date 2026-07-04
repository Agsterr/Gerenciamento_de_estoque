#!/usr/bin/env bash
set -euo pipefail
ESTOQUE_ENV="${ESTOQUE_ENV:-/opt/gerenciamento-estoque/.env}"
API_TOKEN="${CLOUDFLARE_API_TOKEN:-}"
if [[ -z "$API_TOKEN" && -f /opt/app-rotas/.env ]]; then
  API_TOKEN="$(grep '^CLOUDFLARE_API_TOKEN=' /opt/app-rotas/.env | cut -d= -f2- | tr -d '\r\n' || true)"
fi
if [[ -z "$API_TOKEN" ]]; then
  echo "ERRO: CLOUDFLARE_API_TOKEN ausente."
  exit 1
fi
export CLOUDFLARE_API_TOKEN="$API_TOKEN"
export ESTOQUE_ENV
python3 <<'PY'
import json, base64, os, urllib.request

env = os.environ["ESTOQUE_ENV"]
token_cf = os.environ["CLOUDFLARE_API_TOKEN"]
token_tunnel = None
for line in open(env):
    if line.startswith("CLOUDFLARED_TUNNEL_TOKEN="):
        token_tunnel = line.split("=", 1)[1].strip()
        break
raw = token_tunnel + "=" * (-len(token_tunnel) % 4)
data = json.loads(base64.urlsafe_b64decode(raw))
account_id, tunnel_id = data["a"], data["t"]
payload = {
  "config": {
    "ingress": [
      {"hostname": "focodev.com.br", "service": "http://localhost:80"},
      {"hostname": "www.focodev.com.br", "service": "http://127.0.0.1:8082"},
      {"hostname": "painel.focodev.com.br", "service": "http://127.0.0.1:8082"},
      {"hostname": "rotas.focodev.com.br", "service": "http://127.0.0.1:8081"},
      {"hostname": "admin.rotas.focodev.com.br", "service": "http://127.0.0.1:8081"},
      {"hostname": "admin-rotas.focodev.com.br", "service": "http://127.0.0.1:8081"},
      {"hostname": "portal-rotas.focodev.com.br", "service": "http://127.0.0.1:8081"},
      {"hostname": "mercado.focodev.com.br", "service": "http://127.0.0.1:8083"},
      {"hostname": "admin.mercado.focodev.com.br", "service": "http://127.0.0.1:8083"},
      {"service": "http_status:404"},
    ]
  }
}
url = f"https://api.cloudflare.com/client/v4/accounts/{account_id}/cfd_tunnel/{tunnel_id}/configurations"
req = urllib.request.Request(
    url,
    data=json.dumps(payload).encode(),
    method="PUT",
    headers={"Authorization": f"Bearer {token_cf}", "Content-Type": "application/json"},
)
try:
    with urllib.request.urlopen(req) as resp:
        body = json.loads(resp.read().decode())
except urllib.error.HTTPError as e:
    err = e.read().decode()
    raise SystemExit(f"HTTP {e.code}: {err}")
if not body.get("success"):
    raise SystemExit(body)
print("OK: ingress remoto atualizado (www -> 8082)")
PY
