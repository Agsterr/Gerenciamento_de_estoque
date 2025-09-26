# 📦 **Sistema de Gerenciamento de Estoque - API REST**

Este projeto é uma **API REST** desenvolvida em **Spring Boot** para o **gerenciamento de estoque**.
O sistema permite o controle de **produtos, consumidores, movimentações de estoque e entregas**, incluindo **autenticação segura de usuários, relatórios, integração com pagamentos e documentação interativa da API**.

---

## 🚀 **Funcionalidades**

### 🛒 **Gestão de Produtos**

* Cadastro, atualização, listagem e exclusão de produtos.
* Controle de quantidade em estoque.

### 👥 **Gestão de Consumidores**

* Cadastro e gerenciamento de clientes/consumidores.

### 📦 **Gestão de Entregas**

* Registro de entregas associadas a consumidores e produtos.

### 🔄 **Movimentação de Estoque**

* Entrada e saída de produtos.
* Histórico de movimentações.

### 🔒 **Autenticação & Segurança**

* Login com usuário e senha.
* Implementação de **Spring Security com JWT (JSON Web Token)**.
* Controle de acessos por perfis de usuário.
* Configuração de **CORS** para integração com front-end.

### ✅ **Validações**

* Uso de **Bean Validation** para garantir integridade dos dados recebidos pela API.

### 📊 **Relatórios**

* Geração de relatórios com **JasperReports** (PDF/Excel).

### 🏢 **Multi-tenant**

* Suporte a múltiplos clientes (multi-tenancy), permitindo separar os dados por empresa/usuário.

### 💳 **Integração com Pagamentos (Stripe)**

* Integração com **Stripe API** para pagamentos online.
* Suporte a cobrança de entregas, movimentações ou mensalidades.
* Estrutura flexível para expansão de planos pagos.

### 📖 **Documentação da API (Swagger/OpenAPI)**

* Geração automática de documentação interativa com **SpringDoc OpenAPI**.
* Endpoints da API descritos com suporte a autenticação via JWT.
* Testes rápidos via Swagger UI.

---

## 🛠️ **Tecnologias Utilizadas**

* ☕ **Java 17+**
* 🌱 **Spring Boot**
* 🗃️ **Spring Data JPA**
* 🔒 **Spring Security + JWT**
* 🌀 **Hibernate (JPA)**
* ✅ **Bean Validation (Jakarta Validation)**
* 🐘 **PostgreSQL**
* 📊 **JasperReports**
* 💳 **Stripe API**
* 📖 **SpringDoc OpenAPI (Swagger)**
* 🔗 **CORS Configuration**
* 🏢 **Multi-tenancy Strategy**

---

## 📂 **Principais Módulos da API**

* `/api/produtos` → Operações de produtos
* `/api/consumidores` → Operações de consumidores
* `/api/entregas` → Operações de entregas
* `/api/movimentacoes` → Controle de entradas/saídas de estoque
* `/api/auth` → Autenticação (login e cadastro de usuários)
* `/api/relatorios` → Geração de relatórios em PDF
* `/api/pagamentos` → Integração com Stripe

---

## 🔑 **Autenticação**

A autenticação é realizada via **JWT**.

**Fluxo básico:**

1. Usuário realiza login (`/api/auth/login`) enviando **nome de usuário e senha**.
2. A API retorna um **token JWT**.
3. O token deve ser enviado no header:

   ```
   Authorization: Bearer <token>
   ```

   para acessar as rotas protegidas.

---

## 📊 **Relatórios**

* Relatório de produtos em estoque.
* Relatório de movimentações.
* Relatório de entregas.

Formatos: **PDF / Excel** via **JasperReports**.

---

## 💳 **Integração Stripe**

* Endpoint `/api/pagamentos` para iniciar e processar transações.
* Suporte a pagamentos online diretamente pela API.
* Uso das bibliotecas oficiais do **Stripe para Java**.
* Estrutura preparada para expansão futura (**planos de assinatura, checkout etc.**).

---

## 📖 **Documentação da API - Swagger**

A API conta com documentação interativa via **SpringDoc OpenAPI**.

No **Swagger UI** é possível:

* Visualizar todos os endpoints da API.
* Testar requisições autenticadas com JWT.
* Baixar a especificação OpenAPI em **JSON** ou **YAML**.

---

## ⚙️ **Configuração do Projeto**

### 🔧 Pré-requisitos

* Java 17+
* Maven
* PostgreSQL
* Conta no Stripe (para configurar as chaves de API).

### 🚀 Passos

1. Clonar o repositório:

   ```bash
   git clone https://github.com/seu-usuario/seu-repositorio.git
   ```

2. Configurar o banco no `application.properties` ou `application.yml`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/estoque
   spring.datasource.username=seu-usuario
   spring.datasource.password=sua-senha
   ```

3. Configurar as credenciais do Stripe:

   ```properties
   stripe.api.key=sk_test_sua_chave_aqui
   stripe.webhook.secret=whsec_sua_chave_aqui
   ```

4. Executar a aplicação:

   ```bash
   mvn spring-boot:run
   ```

---

## 🔑 **Multi-tenancy**

* Suporte a múltiplos clientes (**multi-tenant**).
* Cada requisição pode ser associada a um **tenant específico**, garantindo separação lógica dos dados.

---

## 📌 **Observações**

* Projeto **backend (API REST)**, ideal para consumo por aplicações **front-end** (Angular, React, Vue ou mobile).
* Relatórios exportados em **PDF** (adaptável para outros formatos).
* **JWT + Spring Security** garantem segurança e escalabilidade.
* **Stripe** adiciona flexibilidade para monetização e pagamentos.
* **Swagger** facilita testes e integração com clientes e parceiros.

---

## 🌐 **Deploy e Hospedagem**

* A aplicação está hospedada na plataforma **Render**.
* Integração contínua (**CI/CD**) configurada com **GitHub**.
* A cada **push** na branch principal, o Render executa automaticamente o **build** e realiza o **deploy**.
* O ambiente de produção é atualizado de forma automática e transparente.

---

## 📖 **Próximos Passos**

* 📧 Implementação de notificações automáticas por e-mail.
* 📊 Dashboard com estatísticas de estoque.
* 🌍 Expansão do suporte multi-tenant para subdomínios.
* 💳 Integração com **Stripe Hosted Checkout Page**.

---

✍️ **Autor:** *Agster Junior da Costa Santos*
