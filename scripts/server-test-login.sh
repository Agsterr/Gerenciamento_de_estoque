#!/bin/bash
JSON='{"username":"admin","senha":"admin123"}'
echo "$JSON" > /tmp/login.json
echo "=== localhost ==="
curl -s -w "\nHTTP:%{http_code}\n" -X POST http://127.0.0.1/auth/login -H 'Content-Type: application/json' --data-binary @/tmp/login.json
echo "=== public https ==="
curl -s -w "\nHTTP:%{http_code}\n" -X POST https://focodev.com.br/auth/login -H 'Content-Type: application/json' --data-binary @/tmp/login.json
echo "=== nginx access (last login posts) ==="
docker logs gerenciamento-nginx --tail 30 2>&1 | grep auth/login || true
