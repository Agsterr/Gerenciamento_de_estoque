# Smoke test da API — Gerenciamento de Estoque
# Uso: .\scripts\test-api.ps1 [-BaseUrl "https://focodev.com.br"] [-User admin] [-Senha admin123] [-SkipAuth]
param(
    [string]$BaseUrl = "http://localhost",
    [string]$User = "admin",
    [string]$Senha = "admin123",
    [switch]$SkipAuth
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")

function Test-Get {
    param([string]$Path, [hashtable]$Headers = @{})
    $url = "$base$Path"
    Write-Host "GET $url" -ForegroundColor Cyan
    try {
        $r = Invoke-WebRequest -Uri $url -Headers $Headers -UseBasicParsing
        Write-Host "  -> $($r.StatusCode)" -ForegroundColor Green
        if ($r.Content.Length -lt 500) { Write-Host "  $($r.Content)" }
        else { Write-Host "  ($($r.Content.Length) bytes)" }
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Write-Host "  -> $code $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== Publicos ===" -ForegroundColor Yellow
Test-Get "/actuator/health"
Test-Get "/api/asaas/config"
Test-Get "/api/plans"

if ($SkipAuth) {
    Write-Host "`nAuth ignorado (-SkipAuth)." -ForegroundColor Yellow
    exit 0
}

Write-Host "`n=== Login ===" -ForegroundColor Yellow
$loginBody = (@{ username = $User; senha = $Senha } | ConvertTo-Json -Compress)
try {
    $login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType "application/json" -Body $loginBody
    Write-Host "  -> 200 token obtido" -ForegroundColor Green
} catch {
    Write-Host "  -> login falhou: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{ Authorization = "Bearer $($login.token)" }

Write-Host "`n=== Autenticados ===" -ForegroundColor Yellow
Test-Get "/produtos" $headers
Test-Get "/categorias" $headers
Test-Get "/api/subscription/status" $headers

Write-Host "`nConcluido." -ForegroundColor Green
