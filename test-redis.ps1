# ========================================
# Script de Teste Redis
# ========================================

Write-Host "🧪 Testando Redis..." -ForegroundColor Cyan
Write-Host ""

# Verificar se Redis está instalado
$redisPath = "C:\Program Files\Redis"
if (-not (Test-Path "$redisPath\redis-server.exe")) {
    Write-Host "❌ Redis não encontrado!" -ForegroundColor Red
    Write-Host "   Execute primeiro: .\install-redis.ps1" -ForegroundColor Yellow
    exit 1
}

# Verificar se Redis está rodando
$redisProcess = Get-Process -Name "redis-server" -ErrorAction SilentlyContinue
if (-not $redisProcess) {
    Write-Host "⚠️  Redis não está rodando. Iniciando..." -ForegroundColor Yellow
    Start-Process -FilePath "$redisPath\redis-server.exe" -WindowStyle Minimized
    Start-Sleep -Seconds 3
    
    $redisProcess = Get-Process -Name "redis-server" -ErrorAction SilentlyContinue
    if (-not $redisProcess) {
        Write-Host "❌ Falha ao iniciar Redis" -ForegroundColor Red
        exit 1
    }
}

Write-Host "✅ Redis está rodando (PID: $($redisProcess.Id))" -ForegroundColor Green

# Teste de conectividade
Write-Host "🔗 Testando conectividade..." -ForegroundColor Yellow

try {
    # Teste PING
    $pingResult = & "$redisPath\redis-cli.exe" ping 2>$null
    if ($pingResult -eq "PONG") {
        Write-Host "✅ PING → PONG" -ForegroundColor Green
    } else {
        Write-Host "❌ PING falhou" -ForegroundColor Red
        exit 1
    }
    
    # Teste SET/GET
    Write-Host "🔧 Testando operações SET/GET..." -ForegroundColor Yellow
    
    $setResult = & "$redisPath\redis-cli.exe" set "test:key" "test-value" 2>$null
    if ($setResult -eq "OK") {
        Write-Host "✅ SET test:key → OK" -ForegroundColor Green
    } else {
        Write-Host "❌ SET falhou" -ForegroundColor Red
    }
    
    $getValue = & "$redisPath\redis-cli.exe" get "test:key" 2>$null
    if ($getValue -eq "test-value") {
        Write-Host "✅ GET test:key → test-value" -ForegroundColor Green
    } else {
        Write-Host "❌ GET falhou" -ForegroundColor Red
    }
    
    # Limpar chave de teste
    & "$redisPath\redis-cli.exe" del "test:key" 2>$null | Out-Null
    
    # Informações do servidor
    Write-Host "📊 Informações do Redis:" -ForegroundColor Cyan
    $info = & "$redisPath\redis-cli.exe" info server 2>$null
    $version = ($info | Select-String "redis_version:(.*)" | ForEach-Object { $_.Matches[0].Groups[1].Value })
    $uptime = ($info | Select-String "uptime_in_seconds:(.*)" | ForEach-Object { $_.Matches[0].Groups[1].Value })
    
    if ($version) {
        Write-Host "   Versão: $version" -ForegroundColor Gray
    }
    if ($uptime) {
        $uptimeMin = [math]::Round([int]$uptime / 60, 1)
        Write-Host "   Uptime: $uptimeMin minutos" -ForegroundColor Gray
    }
    
    # Verificar memória
    $memInfo = & "$redisPath\redis-cli.exe" info memory 2>$null
    $usedMemory = ($memInfo | Select-String "used_memory_human:(.*)" | ForEach-Object { $_.Matches[0].Groups[1].Value })
    if ($usedMemory) {
        Write-Host "   Memória usada: $usedMemory" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "🎉 Redis está funcionando perfeitamente!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Próximos passos para ativar na aplicação:" -ForegroundColor Cyan
    Write-Host "   1. Edite: src/main/resources/application-local.properties" -ForegroundColor Gray
    Write-Host "   2. Altere: spring.cache.type=redis" -ForegroundColor Gray
    Write-Host "   3. Altere: spring.data.redis.enabled=true" -ForegroundColor Gray
    Write-Host "   4. Reinicie a aplicação Spring Boot" -ForegroundColor Gray
    Write-Host ""
    Write-Host "🔧 Comandos úteis:" -ForegroundColor Cyan
    Write-Host "   redis-cli monitor        # Monitorar comandos em tempo real" -ForegroundColor Gray
    Write-Host "   redis-cli info           # Informações detalhadas" -ForegroundColor Gray
    Write-Host "   redis-cli keys '*'       # Listar todas as chaves" -ForegroundColor Gray
    Write-Host "   redis-cli flushall       # Limpar todos os dados" -ForegroundColor Gray
    
} catch {
    Write-Host "❌ Erro durante os testes: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Pressione qualquer tecla para continuar..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")