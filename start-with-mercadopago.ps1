# Script para iniciar a aplicação com variáveis de ambiente do Mercado Pago
# Este script define as variáveis no terminal atual e inicia a aplicação

Write-Host "=== Configurando variáveis de ambiente do Mercado Pago ===" -ForegroundColor Cyan

# Verifica se as variáveis estão definidas no sistema
$testToken = [Environment]::GetEnvironmentVariable("MERCADOPAGO_TEST_ACCESS_TOKEN", "User")
$testKey = [Environment]::GetEnvironmentVariable("MERCADOPAGO_TEST_PUBLIC_KEY", "User")

if ($testToken) {
    Write-Host "✓ MERCADOPAGO_TEST_ACCESS_TOKEN encontrada no sistema" -ForegroundColor Green
    $env:MERCADOPAGO_TEST_ACCESS_TOKEN = $testToken
} else {
    Write-Host "✗ MERCADOPAGO_TEST_ACCESS_TOKEN não encontrada" -ForegroundColor Yellow
    Write-Host "  Defina a variável no sistema ou neste script" -ForegroundColor Yellow
}

if ($testKey) {
    Write-Host "✓ MERCADOPAGO_TEST_PUBLIC_KEY encontrada no sistema" -ForegroundColor Green
    $env:MERCADOPAGO_TEST_PUBLIC_KEY = $testKey
} else {
    Write-Host "✗ MERCADOPAGO_TEST_PUBLIC_KEY não encontrada" -ForegroundColor Yellow
    Write-Host "  Defina a variável no sistema ou neste script" -ForegroundColor Yellow
}

# Define o ambiente como 'test' por padrão se não estiver definido
if (-not $env:MERCADOPAGO_ENVIRONMENT) {
    $env:MERCADOPAGO_ENVIRONMENT = "test"
    Write-Host "✓ MERCADOPAGO_ENVIRONMENT definida como 'test'" -ForegroundColor Green
}

# Define outras variáveis opcionais se não estiverem definidas
if (-not $env:MERCADOPAGO_SUCCESS_URL) {
    $env:MERCADOPAGO_SUCCESS_URL = "http://localhost:8080/subscription/success"
}
if (-not $env:MERCADOPAGO_CANCEL_URL) {
    $env:MERCADOPAGO_CANCEL_URL = "http://localhost:8080/subscription/cancel"
}
if (-not $env:MERCADOPAGO_PENDING_URL) {
    $env:MERCADOPAGO_PENDING_URL = "http://localhost:8080/subscription/pending"
}

Write-Host ""
Write-Host "=== Variáveis configuradas ===" -ForegroundColor Cyan
Write-Host "MERCADOPAGO_ENVIRONMENT: $env:MERCADOPAGO_ENVIRONMENT"
Write-Host "MERCADOPAGO_TEST_ACCESS_TOKEN: $(if ($env:MERCADOPAGO_TEST_ACCESS_TOKEN) { '***' + $env:MERCADOPAGO_TEST_ACCESS_TOKEN.Substring([Math]::Max(0, $env:MERCADOPAGO_TEST_ACCESS_TOKEN.Length - 4)) } else { '[NÃO DEFINIDA]' })"
Write-Host "MERCADOPAGO_TEST_PUBLIC_KEY: $(if ($env:MERCADOPAGO_TEST_PUBLIC_KEY) { '***' + $env:MERCADOPAGO_TEST_PUBLIC_KEY.Substring([Math]::Max(0, $env:MERCADOPAGO_TEST_PUBLIC_KEY.Length - 4)) } else { '[NÃO DEFINIDA]' })"
Write-Host ""

Write-Host "=== Iniciando aplicação Spring Boot ===" -ForegroundColor Cyan
Write-Host ""

# Inicia a aplicação
.\mvnw.cmd spring-boot:run









