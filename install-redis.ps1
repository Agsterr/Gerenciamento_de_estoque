# ========================================
# Script de Instalação Redis para Windows
# ========================================

Write-Host "🚀 Instalando Redis para Windows..." -ForegroundColor Green

# Verificar se está executando como administrador
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Host "❌ Este script precisa ser executado como Administrador!" -ForegroundColor Red
    Write-Host "   Clique com botão direito no PowerShell e selecione 'Executar como administrador'" -ForegroundColor Yellow
    pause
    exit 1
}

# Configurações
$redisVersion = "3.0.504"
$downloadUrl = "https://github.com/microsoftarchive/redis/releases/download/win-$redisVersion/Redis-x64-$redisVersion.msi"
$tempPath = "$env:TEMP\Redis-x64-$redisVersion.msi"
$installPath = "C:\Program Files\Redis"

try {
    # Verificar se Redis já está instalado
    if (Test-Path "$installPath\redis-server.exe") {
        Write-Host "✅ Redis já está instalado em: $installPath" -ForegroundColor Green
        
        # Verificar se está rodando
        $redisProcess = Get-Process -Name "redis-server" -ErrorAction SilentlyContinue
        if ($redisProcess) {
            Write-Host "✅ Redis já está rodando (PID: $($redisProcess.Id))" -ForegroundColor Green
        } else {
            Write-Host "🔄 Iniciando Redis..." -ForegroundColor Yellow
            Start-Process -FilePath "$installPath\redis-server.exe" -WindowStyle Minimized
            Start-Sleep -Seconds 2
            
            # Verificar se iniciou
            $redisProcess = Get-Process -Name "redis-server" -ErrorAction SilentlyContinue
            if ($redisProcess) {
                Write-Host "✅ Redis iniciado com sucesso!" -ForegroundColor Green
            } else {
                Write-Host "❌ Falha ao iniciar Redis" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "📥 Baixando Redis $redisVersion..." -ForegroundColor Yellow
        
        # Baixar Redis
        Invoke-WebRequest -Uri $downloadUrl -OutFile $tempPath -UseBasicParsing
        Write-Host "✅ Download concluído!" -ForegroundColor Green
        
        Write-Host "🔧 Instalando Redis..." -ForegroundColor Yellow
        
        # Instalar silenciosamente
        Start-Process -FilePath "msiexec.exe" -ArgumentList "/i", $tempPath, "/quiet", "/norestart" -Wait
        
        # Verificar instalação
        if (Test-Path "$installPath\redis-server.exe") {
            Write-Host "✅ Redis instalado com sucesso!" -ForegroundColor Green
            
            # Iniciar Redis
            Write-Host "🔄 Iniciando Redis..." -ForegroundColor Yellow
            Start-Process -FilePath "$installPath\redis-server.exe" -WindowStyle Minimized
            Start-Sleep -Seconds 2
            
            # Verificar se iniciou
            $redisProcess = Get-Process -Name "redis-server" -ErrorAction SilentlyContinue
            if ($redisProcess) {
                Write-Host "✅ Redis iniciado com sucesso!" -ForegroundColor Green
            } else {
                Write-Host "❌ Falha ao iniciar Redis" -ForegroundColor Red
            }
        } else {
            Write-Host "❌ Falha na instalação do Redis" -ForegroundColor Red
            exit 1
        }
        
        # Limpar arquivo temporário
        Remove-Item $tempPath -ErrorAction SilentlyContinue
    }
    
    # Testar conexão
    Write-Host "🧪 Testando conexão Redis..." -ForegroundColor Yellow
    
    if (Test-Path "$installPath\redis-cli.exe") {
        $testResult = & "$installPath\redis-cli.exe" ping 2>$null
        if ($testResult -eq "PONG") {
            Write-Host "✅ Redis está funcionando perfeitamente!" -ForegroundColor Green
            Write-Host "   Teste: redis-cli ping → PONG" -ForegroundColor Gray
        } else {
            Write-Host "⚠️  Redis instalado mas não responde ao ping" -ForegroundColor Yellow
            Write-Host "   Tente reiniciar o serviço Redis" -ForegroundColor Gray
        }
    }
    
    # Adicionar ao PATH se necessário
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($currentPath -notlike "*$installPath*") {
        Write-Host "🔧 Adicionando Redis ao PATH do sistema..." -ForegroundColor Yellow
        [Environment]::SetEnvironmentVariable("Path", "$currentPath;$installPath", "Machine")
        Write-Host "✅ Redis adicionado ao PATH!" -ForegroundColor Green
        Write-Host "   Reinicie o terminal para usar 'redis-cli' e 'redis-server' globalmente" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "🎉 Instalação concluída com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Próximos passos:" -ForegroundColor Cyan
    Write-Host "   1. Redis está rodando na porta 6379" -ForegroundColor Gray
    Write-Host "   2. Configure a aplicação Spring Boot:" -ForegroundColor Gray
    Write-Host "      spring.cache.type=redis" -ForegroundColor Gray
    Write-Host "      spring.data.redis.enabled=true" -ForegroundColor Gray
    Write-Host "   3. Reinicie a aplicação para usar Redis" -ForegroundColor Gray
    Write-Host ""
    Write-Host "🔧 Comandos úteis:" -ForegroundColor Cyan
    Write-Host "   redis-cli ping          # Testar conexão" -ForegroundColor Gray
    Write-Host "   redis-cli info          # Informações do servidor" -ForegroundColor Gray
    Write-Host "   redis-cli keys '*'      # Listar todas as chaves" -ForegroundColor Gray
    Write-Host ""
    
} catch {
    Write-Host "❌ Erro durante a instalação: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "📖 Consulte o guia manual em REDIS_SETUP_GUIDE.md" -ForegroundColor Yellow
    exit 1
}

Write-Host "Pressione qualquer tecla para continuar..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKey