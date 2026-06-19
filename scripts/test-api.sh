#!/bin/bash
# Smoke test da API — uso no servidor ou Linux
# Uso: BASE_URL=https://focodev.com.br ./scripts/test-api.sh

set -euo pipefail
BASE="${BASE_URL:-http://localhost}"
USER="${API_USER:-admin}"
PASS="${API_PASS:-admin123}"

echo "=== Publicos ($BASE) ==="
curl -s -o /dev/null -w "GET /actuator/health -> %{http_code}\n" "$BASE/actuator/health"
curl -s -o /dev/null -w "GET /api/asaas/config -> %{http_code}\n" "$BASE/api/asaas/config"
curl -s -o /dev/null -w "GET /api/plans -> %{http_code}\n" "$BASE/api/plans"

echo "=== Login ==="
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"senha\":\"$PASS\"}")
HTTP=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
echo "POST /auth/login -> $HTTP"
if [ "$HTTP" != "200" ]; then
  echo "$BODY"
  exit 1
fi

TOKEN=$(echo "$BODY" | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "Token nao encontrado na resposta"
  exit 1
fi

echo "=== Autenticados ==="
curl -s -o /dev/null -w "GET /produtos -> %{http_code}\n" -H "Authorization: Bearer $TOKEN" "$BASE/produtos"
curl -s -o /dev/null -w "GET /categorias -> %{http_code}\n" -H "Authorization: Bearer $TOKEN" "$BASE/categorias"
curl -s -o /dev/null -w "GET /api/subscription/status -> %{http_code}\n" -H "Authorization: Bearer $TOKEN" "$BASE/api/subscription/status"

echo "Concluido."
