# Smoke test da API — Gerenciamento de Estoque
# Uso: $env:API_TEST_PASSWORD='sua-senha'; .\scripts\test-api.ps1 [-BaseUrl "https://focodev.com.br"] [-User admin] [-SkipAuth]
param(
    [string]$BaseUrl = "http://localhost",
    [string]$User = "admin",
    [string]$Senha = $env:API_TEST_PASSWORD,
    [switch]$SkipAuth
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")

if (-not $SkipAuth -and [string]::IsNullOrWhiteSpace($Senha)) {
    Write-Error "Defina API_TEST_PASSWORD ou use -SkipAuth."
    exit 1
}

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
    $loginResp = Invoke-WebRequest -Uri "$base/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -UseBasicParsing
    Write-Host "  -> $($loginResp.StatusCode)" -ForegroundColor Green
    $token = ($loginResp.Content | ConvertFrom-Json).token
    if (-not $token) { throw "Token ausente na resposta" }
} catch {
    Write-Host "  -> Falha no login: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== Autenticados ===" -ForegroundColor Yellow
$headers = @{ Authorization = "Bearer $token" }
Test-Get "/produtos" $headers
Test-Get "/categorias" $headers
Test-Get "/api/subscription/status" $headers

Write-Host "`nConcluido." -ForegroundColor Green
