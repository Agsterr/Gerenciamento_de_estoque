# Envia sua chave publica para o servidor Hetzner (pede senha UMA vez)
Write-Host "Conectando em root@178.105.11.19 ..." -ForegroundColor Cyan
Write-Host "Digite a senha do servidor quando solicitado." -ForegroundColor Yellow

Get-Content "$env:USERPROFILE\.ssh\id_rsa.pub" | ssh root@178.105.11.19 "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && echo 'Chave SSH instalada com sucesso!'"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nTestando conexao sem senha..." -ForegroundColor Cyan
    ssh -o BatchMode=yes hetzner "echo OK - login por chave funcionando!"
}
