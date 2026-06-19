#!/bin/sh
set -e

TARGET="${CLOUDFLARED_TARGET_URL:-http://nginx:80}"

if [ -n "${CLOUDFLARED_TUNNEL_TOKEN:-}" ]; then
  echo "cloudflared: iniciando túnel nomeado (URL fixa configurada no painel Cloudflare)"
  exec cloudflared tunnel --no-autoupdate run --token "${CLOUDFLARED_TUNNEL_TOKEN}"
fi

if [ -f /etc/cloudflared/config.yml ]; then
  echo "cloudflared: iniciando túnel via /etc/cloudflared/config.yml"
  exec cloudflared tunnel --no-autoupdate run --config /etc/cloudflared/config.yml
fi

echo "cloudflared: túnel rápido (trycloudflare.com) -> ${TARGET}"
echo "cloudflared: a URL muda a cada restart; use CLOUDFLARED_TUNNEL_TOKEN para URL fixa"
exec cloudflared tunnel --no-autoupdate --url "${TARGET}"
