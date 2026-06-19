# Verifica PostgreSQL local (porta 5432) e, se Docker estiver ativo, o container postgres.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path "$root\docker-compose.yml")) {
    $root = Split-Path -Parent $PSScriptRoot
}

Write-Host "=== Verificacao do banco de dados ===" -ForegroundColor Cyan

$portOk = (Test-NetConnection -ComputerName localhost -Port 5432 -WarningAction SilentlyContinue).TcpTestSucceeded
if ($portOk) {
    Write-Host "[OK] Porta 5432 aberta (PostgreSQL escutando)" -ForegroundColor Green
} else {
    Write-Host "[FALHA] Nada escutando na porta 5432" -ForegroundColor Red
    Write-Host "      Inicie o Postgres local ou: docker compose up -d postgres" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Configuracao esperada (.env local):" -ForegroundColor Cyan
Write-Host "  DB_URL=jdbc:postgresql://localhost:5432/gerenciamento_estoque"
Write-Host "  DB_USER=postgres"
Write-Host "  DB_PASSWORD=postgres"
Write-Host ""
Write-Host "Configuracao Docker (docker-compose sobrescreve no container app):" -ForegroundColor Cyan
Write-Host "  DB_URL=jdbc:postgresql://postgres:5432/gerenciamento_estoque"

$dockerOk = $false
try {
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $dockerOk = $true }
} catch {}

if ($dockerOk) {
    Write-Host ""
    Write-Host "=== Docker ===" -ForegroundColor Cyan
    Push-Location $root
    docker compose ps postgres 2>$null
    $pgContainer = docker compose ps -q postgres 2>$null
    if ($pgContainer) {
        Write-Host "[OK] Container postgres encontrado. Testando conexao..." -ForegroundColor Green
        docker compose exec -T postgres psql -U postgres -d gerenciamento_estoque -c "SELECT current_database();"
    } else {
        Write-Host "[INFO] Container postgres nao esta rodando. Suba com: docker compose up -d postgres" -ForegroundColor Yellow
    }
    Pop-Location
} else {
    Write-Host ""
    Write-Host "[INFO] Docker Desktop nao esta rodando. Verificacao via container ignorada." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Flyway ===" -ForegroundColor Cyan
Write-Host "Migrations em src/main/resources/db/migration (V25 plano 69,90)"
Write-Host "Com FLYWAY_ENABLED=true as migrations rodam na subida do backend."
