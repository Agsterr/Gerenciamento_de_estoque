# Gerenciamento de Estoque

API REST para controle de estoque, vendas e assinaturas — pensada para pequenas e médias empresas que precisam organizar produtos, clientes, movimentações e relatórios em um único sistema.

**Produção:** [focodev.com.br](https://focodev.com.br) (sistema de estoque)  
**Site institucional:** [www.focodev.com.br](https://www.focodev.com.br)  
**Front-end:** [Gerenciamento_de_estoque_front](https://github.com/Agsterr/Gerenciamento_de_estoque_front)

---

## O que o sistema faz

- **Produtos e categorias** — cadastro, estoque e controle de quantidade
- **Consumidores** — gestão de clientes
- **Movimentações** — entradas e saídas com histórico
- **Entregas** — registro vinculado a consumidores e produtos
- **Pedidos de compra e venda** — fluxo comercial completo
- **Organizações (multi-tenant)** — cada empresa tem seus dados isolados
- **Usuários e perfis** — autenticação JWT com controle de acesso por papel
- **Assinaturas e planos** — cobrança recorrente via **Asaas** (e suporte legado ao Mercado Pago)
- **Relatórios** — exportação em PDF com JasperReports
- **Pesquisa de preço e sugestões** — módulos auxiliares para análise
- **Painel administrativo** — gestão de usuários, assinaturas e webhooks

---

## Tecnologias

| Camada | Stack |
|--------|-------|
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Banco | PostgreSQL 15, Flyway (migrações) |
| Cache | Redis (opcional em produção) |
| Auth | JWT (jjwt) |
| Pagamentos | Asaas (principal), Mercado Pago SDK |
| Relatórios | JasperReports |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Infra | Docker, nginx, Cloudflare Tunnel |
| CI/CD | GitHub Actions → deploy no Hetzner |

---

## Estrutura do projeto

```text
Gerenciamento_de_estoque/
├── src/main/java/          # API Spring Boot
├── src/main/resources/     # application.properties, migrações Flyway
├── docker/                 # nginx, secrets, backup
├── scripts/                # deploy e setup no servidor
├── docker-compose.yml      # Stack local e produção
└── Dockerfile
```

---

## Como rodar localmente

### Pré-requisitos

- Java 17+
- Maven 3.8+
- Docker e Docker Compose (recomendado)

### Com Docker (mais simples)

```bash
git clone https://github.com/Agsterr/Gerenciamento_de_estoque.git
cd Gerenciamento_de_estoque
docker compose up --build
```

Para produção ou variáveis customizadas, crie um arquivo `.env` na raiz (veja [docs/DEPLOY_DOCKER.md](docs/DEPLOY_DOCKER.md)).

A API sobe em `http://localhost:8080`. O nginx expõe a porta `80`.

### Sem Docker

```bash
mvn spring-boot:run
```

Configure o PostgreSQL em `src/main/resources/application.properties` (ou via variáveis de ambiente).

---

## Documentação da API

Com a aplicação rodando, acesse:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

A maioria dos endpoints exige autenticação. Faça login e envie o token no header:

```http
Authorization: Bearer <seu-token-jwt>
```

---

## Principais endpoints

| Módulo | Rota base |
|--------|-----------|
| Autenticação | `/api/auth` |
| Produtos | `/produtos` |
| Consumidores | `/consumidores` |
| Movimentações | `/movimentacoes` |
| Entregas | `/entregas` |
| Pedidos de venda | `/pedidos-venda` |
| Pedidos de compra | `/pedidos-compra` |
| Organizações | `/api/orgs` |
| Assinaturas | `/api/subscription` |
| Planos | `/api/plans` |
| Relatórios | `/relatorios` |
| Webhooks | `/api/webhooks` |
| Admin | `/admin` |

---

## Deploy em produção

O sistema roda no **Hetzner** com Docker Compose, nginx e túnel **Cloudflare**:

| Item | Valor |
|------|-------|
| Servidor | `178.105.11.19` |
| Path no servidor | `/opt/gerenciamento-estoque` |
| Domínio (estoque) | `https://focodev.com.br` |
| Site institucional | `https://www.focodev.com.br` |

Deploy automático: push na branch `main` dispara o workflow do GitHub Actions.

```bash
# Deploy manual (no servidor)
ssh hetzner "bash /opt/gerenciamento-estoque/scripts/deploy-hetzner.sh"
```

---

## Testes

```bash
mvn test
```

---

## Autor

**Agster Junior da Costa Santos** — [GitHub](https://github.com/Agsterr)
