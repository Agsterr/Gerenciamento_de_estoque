# 🚀 Guia Completo de Instalação Redis - Windows

## 🔍 Problema Identificado

**Docker Desktop não está funcionando:**
- Docker instalado mas não consegue iniciar
- Erro: "Docker Desktop is unable to start"
- Necessário alternativas para Redis

---

## 📋 Múltiplas Soluções Disponíveis

### 🎯 **Opção 1: Redis Nativo Windows (Recomendado)**

#### **🚀 Instalação Automática (Mais Fácil):**
```powershell
# Execute como Administrador:
.\install-redis.ps1
```

**Este script irá:**
- ✅ Baixar Redis 3.0.504 automaticamente
- ✅ Instalar silenciosamente
- ✅ Iniciar o serviço Redis
- ✅ Adicionar ao PATH do sistema
- ✅ Testar a conectividade
- ✅ Fornecer instruções de configuração

#### **Via Chocolatey (Alternativa):**
```powershell
# 1. Instalar Chocolatey (se não tiver)
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# 2. Instalar Redis
choco install redis-64 -y

# 3. Iniciar Redis
redis-server
```

#### **Download Manual:**
1. **Baixar Redis para Windows:**
   - Acesse: https://github.com/microsoftarchive/redis/releases
   - Baixe: `Redis-x64-3.0.504.msi`
   - Instale normalmente

2. **Iniciar Redis:**
   ```powershell
   # Navegar para pasta de instalação
   cd "C:\Program Files\Redis"
   
   # Iniciar servidor
   redis-server.exe
   ```

3. **Testar Conexão:**
   ```powershell
   # Em outro terminal
   redis-cli.exe ping
   # Deve retornar: PONG
   ```

---

### 🐧 **Opção 2: Redis via WSL (Windows Subsystem for Linux)**

#### **Instalar WSL:**
```powershell
# Habilitar WSL
wsl --install

# Reiniciar o computador
# Instalar Ubuntu
wsl --install -d Ubuntu
```

#### **Instalar Redis no WSL:**
```bash
# Dentro do WSL/Ubuntu
sudo apt update
sudo apt install redis-server

# Iniciar Redis
sudo service redis-server start

# Testar
redis-cli ping
```

#### **Configurar Acesso do Windows:**
```bash
# Editar configuração Redis
sudo nano /etc/redis/redis.conf

# Alterar:
bind 127.0.0.1 0.0.0.0
protected-mode no

# Reiniciar
sudo service redis-server restart
```

---

### ☁️ **Opção 3: Redis Cloud (Produção)**

#### **Redis Cloud (Gratuito até 30MB):**
1. Acesse: https://redis.com/try-free/
2. Crie conta gratuita
3. Crie database Redis
4. Copie as credenciais:
   - Host: `redis-xxxxx.c1.us-east-1-2.ec2.cloud.redislabs.com`
   - Port: `12345`
   - Password: `sua-senha`

#### **AWS ElastiCache (Pago):**
1. Console AWS → ElastiCache
2. Create Redis Cluster
3. Configure security groups
4. Obtenha endpoint

---

## ⚙️ Configuração da Aplicação

### **1. Habilitar Redis na Aplicação:**

#### **Arquivo: `application-local.properties`**
```properties
# =============================
# Cache & Redis Configuration
# =============================
# Habilitar Redis
spring.cache.type=redis
spring.data.redis.enabled=true

# Configuração Redis Local
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.database=0
spring.data.redis.timeout=2000ms

# Pool de conexões
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0

# TTL do cache
spring.cache.redis.time-to-live=1800000
```

#### **Para Redis Cloud:**
```properties
# Redis Cloud
spring.data.redis.host=redis-xxxxx.c1.us-east-1-2.ec2.cloud.redislabs.com
spring.data.redis.port=12345
spring.data.redis.password=sua-senha-aqui
spring.data.redis.ssl=true
```

#### **Para WSL:**
```properties
# Redis no WSL
spring.data.redis.host=localhost
spring.data.redis.port=6379
# ou usar IP do WSL: 172.x.x.x
```

### **2. Variáveis de Ambiente (.env):**
```env
# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
CACHE_TYPE=redis
```

---

## 🧪 Testar Redis

### **Teste Automático (Recomendado):**
```powershell
# Execute o script de teste:
.\test-redis.ps1
```

**Este script irá:**
- ✅ Verificar se Redis está instalado
- ✅ Iniciar Redis se não estiver rodando
- ✅ Testar PING/PONG
- ✅ Testar operações SET/GET
- ✅ Mostrar informações do servidor
- ✅ Fornecer próximos passos

### **1. Testar Conexão:**
```powershell
# Redis nativo
redis-cli ping

# WSL
wsl redis-cli ping

# Com senha
redis-cli -h host -p port -a password ping
```

### **2. Testar na Aplicação:**
```bash
# Iniciar aplicação com perfil local
mvn spring-boot:run -Dspring.profiles.active=local

# Verificar logs para:
# "Lettuce connection established"
# "Redis cache manager initialized"
```

### **3. Endpoints de Monitoramento:**
```bash
# Health check
curl http://localhost:8081/actuator/health

# Cache info
curl http://localhost:8081/actuator/cache

# Métricas
curl http://localhost:8081/actuator/metrics
```

---

## 🔧 Troubleshooting

### **Problemas Comuns:**

#### **1. "Connection refused":**
```bash
# Verificar se Redis está rodando
netstat -an | findstr :6379

# Iniciar Redis
redis-server
# ou
sudo service redis-server start
```

#### **2. "Authentication failed":**
```properties
# Verificar senha no application.properties
spring.data.redis.password=sua-senha
```

#### **3. "Timeout":**
```properties
# Aumentar timeout
spring.data.redis.timeout=5000ms
```

#### **4. Fallback para Cache Simples:**
```properties
# Se Redis falhar, usar cache simples
spring.cache.type=simple
spring.data.redis.enabled=false
```

---

## 📊 Monitoramento

### **Redis CLI Commands:**
```bash
# Informações do servidor
redis-cli info

# Listar chaves
redis-cli keys "*"

# Monitorar comandos
redis-cli monitor

# Estatísticas
redis-cli info stats
```

### **Logs da Aplicação:**
```properties
# Habilitar logs Redis
logging.level.org.springframework.data.redis=DEBUG
logging.level.io.lettuce.core=DEBUG
```

---

## 🎯 Recomendações

### **Para Desenvolvimento:**
1. **Redis Nativo Windows** (via Chocolatey)
2. Configuração simples e rápida
3. Fácil de gerenciar

### **Para Produção:**
1. **Redis Cloud** ou **AWS ElastiCache**
2. Backup automático
3. Alta disponibilidade
4. Monitoramento integrado

### **Para Testes:**
1. **WSL Redis** ou **Docker** (quando funcionar)
2. Ambiente isolado
3. Fácil reset

---

## ✅ Próximos Passos

1. **Escolher uma opção** de instalação
2. **Instalar Redis** seguindo o guia
3. **Configurar aplicação** com as propriedades corretas
4. **Testar conectividade** com redis-cli
5. **Iniciar aplicação** e verificar logs
6. **Monitorar performance** via actuator

---

## 🆘 Suporte

Se encontrar problemas:
1. Verificar logs da aplicação
2. Testar Redis CLI
3. Verificar configurações
4. Usar fallback para cache simples
5. Consultar documentação oficial Redis

**A aplicação sempre funcionará com cache simples como fallback, garantindo robustez mesmo sem Redis.**