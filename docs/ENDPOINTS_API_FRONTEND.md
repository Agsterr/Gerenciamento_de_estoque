# Documentação Completa de Endpoints da API - Frontend

Este documento contém todos os endpoints da API que são utilizados pelo frontend Angular. Use este documento como referência para verificar se todas as integrações estão corretas.

**Base URL:** `https://gerenciamento-de-estoque-mw08.onrender.com`

**Autenticação:** A maioria dos endpoints requer autenticação via JWT token no header:
```
Authorization: Bearer {token}
```

O token é obtido através do endpoint `/auth/login` e armazenado no `localStorage` com a chave `jwtToken`.

---

## 1. Autenticação (`/auth`)

### 1.1. Login
- **Método:** `POST`
- **Endpoint:** `/auth/login`
- **Autenticação:** Não requerida
- **Body:**
```json
{
  "username": "string",
  "senha": "string",
  "orgId": number
}
```
- **Resposta (200 OK):**
```json
{
  "token": "string (JWT)",
  "roles": ["string"]
}
```
- **Service:** `AuthService.login()`

### 1.2. Registro de Usuário
- **Método:** `POST`
- **Endpoint:** `/auth/register`
- **Autenticação:** Requerida (Bearer Token)
- **Body:**
```json
{
  "username": "string",
  "senha": "string",
  "email": "string",
  "roles": ["string"],
  "orgId": number
}
```
- **Resposta (200 OK):**
```json
{
  "id": number,
  "username": "string",
  "email": "string",
  "roles": ["string"],
  "orgId": number
}
```
- **Service:** `AuthService.register()`

---

## 2. Organizações (`/api/orgs`)

### 2.1. Listar Todas as Organizações
- **Método:** `GET`
- **Endpoint:** `/api/orgs`
- **Autenticação:** Não requerida
- **Resposta (200 OK):**
```json
[
  {
    "id": number,
    "nome": "string",
    "ativo": boolean
  }
]
```
- **Service:** `OrgService.getAll()`

### 2.2. Buscar Organização por ID
- **Método:** `GET`
- **Endpoint:** `/api/orgs/{id}`
- **Autenticação:** Não requerida
- **Parâmetros Path:**
  - `id` (number): ID da organização
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string",
  "ativo": boolean
}
```
- **Service:** `OrgService.getById(id)`

### 2.3. Criar Organização
- **Método:** `POST`
- **Endpoint:** `/api/orgs`
- **Autenticação:** Não requerida
- **Body:**
```json
{
  "nome": "string"
}
```
- **Resposta (201 Created):**
```json
{
  "id": number,
  "nome": "string",
  "ativo": boolean
}
```
- **Resposta (409 Conflict):**
```json
{
  "error": "Já existe uma organização com este nome."
}
```
- **Service:** `OrgService.create(nome)`

### 2.4. Atualizar Organização
- **Método:** `PUT`
- **Endpoint:** `/api/orgs/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da organização
- **Body:**
```json
{
  "nome": "string"
}
```
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string",
  "ativo": boolean
}
```
- **Service:** `OrgService.update(id, nome)`

### 2.5. Ativar Organização
- **Método:** `PUT`
- **Endpoint:** `/api/orgs/{id}/ativar`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da organização
- **Body:** Vazio `{}`
- **Resposta (204 No Content)**
- **Service:** `OrgService.ativar(id)`

### 2.6. Desativar Organização
- **Método:** `PUT`
- **Endpoint:** `/api/orgs/{id}/desativar`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da organização
- **Body:** Vazio `{}`
- **Resposta (204 No Content)`
- **Service:** `OrgService.desativar(id)`

---

## 3. Usuários (`/usuarios`)

### 3.1. Listar Usuários Ativos
- **Método:** `GET`
- **Endpoint:** `/usuarios/ativos?orgId={orgId}&page={page}&size={size}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Query Parameters:**
  - `orgId` (number, obrigatório): ID da organização
  - `page` (number, opcional): Número da página (padrão: 0)
  - `size` (number, opcional): Tamanho da página (padrão: 20)
- **Resposta (200 OK):**
```json
[
  {
    "id": number,
    "username": "string",
    "email": "string",
    "roles": ["string"],
    "ativo": boolean
  }
]
```
- **Resposta (204 No Content):** Quando não há usuários

### 3.2. Ativar Usuário
- **Método:** `PUT`
- **Endpoint:** `/usuarios/{id}/ativar?orgId={orgId}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do usuário
- **Query Parameters:**
  - `orgId` (number, obrigatório): ID da organização
- **Resposta (200 OK):**
```json
{
  "message": "Usuário ativado com sucesso!"
}
```

### 3.3. Desativar Usuário
- **Método:** `PUT`
- **Endpoint:** `/usuarios/{id}/desativar?orgId={orgId}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Parâmetros Path:**
  - `id` (number): ID do usuário
- **Query Parameters:**
  - `orgId` (number, obrigatório): ID da organização
- **Resposta (200 OK):**
```json
{
  "message": "Usuário desativado com sucesso!"
}
```

### 3.4. Reativar Usuário
- **Método:** `POST`
- **Endpoint:** `/usuarios/reativar-usuario?orgId={orgId}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Query Parameters:**
  - `orgId` (number, obrigatório): ID da organização
- **Body:**
```json
{
  "username": "string"
}
```
- **Resposta (200 OK):**
```json
{
  "message": "Usuário reativado com sucesso."
}
```

---

## 4. Produtos (`/produtos`)

### 4.1. Listar Produtos (Paginado)
- **Método:** `GET`
- **Endpoint:** `/produtos?page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `page` (number, opcional): Número da página (padrão: 0)
  - `size` (number, opcional): Tamanho da página (padrão: 10)
- **Resposta (200 OK):**
```json
{
  "content": [
    {
      "id": number,
      "nome": "string",
      "descricao": "string",
      "quantidade": number,
      "quantidadeMinima": number,
      "preco": number,
      "categoria": {
        "id": number,
        "nome": "string"
      }
    }
  ],
  "totalPages": number,
  "totalElements": number,
  "number": number,
  "size": number
}
```
- **Service:** `ProdutoService.listarProdutos(page, size)`
- **Nota:** O `orgId` é extraído automaticamente do JWT token

### 4.2. Buscar Produto por ID
- **Método:** `GET`
- **Endpoint:** `/produtos/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do produto
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string",
  "descricao": "string",
  "quantidade": number,
  "quantidadeMinima": number,
  "preco": number,
  "categoria": {
    "id": number,
    "nome": "string"
  }
}
```
- **Service:** `ProdutoService.getProdutoById(produtoId)`

### 4.3. Criar Produto
- **Método:** `POST`
- **Endpoint:** `/produtos`
- **Autenticação:** Requerida
- **Body:**
```json
{
  "nome": "string",
  "descricao": "string",
  "quantidade": number,
  "quantidadeMinima": number,
  "preco": number,
  "categoriaId": number
}
```
- **Resposta (200 OK):**
```json
{
  "message": "Produto criado ou atualizado com sucesso!"
}
```
- **Service:** `ProdutoService.criarProduto(produto)`

### 4.4. Atualizar Produto
- **Método:** `PUT`
- **Endpoint:** `/produtos/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do produto
- **Body:**
```json
{
  "nome": "string",
  "descricao": "string",
  "quantidade": number,
  "quantidadeMinima": number,
  "preco": number,
  "categoriaId": number,
  "orgId": number
}
```
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string",
  "descricao": "string",
  "quantidade": number,
  "quantidadeMinima": number,
  "preco": number,
  "categoria": {
    "id": number,
    "nome": "string"
  }
}
```
- **Service:** `ProdutoService.atualizarProduto(produto, id)`

### 4.5. Deletar Produto
- **Método:** `DELETE`
- **Endpoint:** `/produtos/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do produto
- **Resposta (200 OK):**
```json
{
  "message": "Produto excluído com sucesso."
}
```
- **Service:** `ProdutoService.deletarProduto(produtoId)`

### 4.6. Listar Produtos com Estoque Baixo
- **Método:** `GET`
- **Endpoint:** `/produtos/estoque-baixo`
- **Autenticação:** Requerida
- **Resposta (200 OK):**
```json
[
  {
    "id": number,
    "nome": "string",
    "descricao": "string",
    "quantidade": number,
    "quantidadeMinima": number,
    "preco": number,
    "categoria": {
      "id": number,
      "nome": "string"
    }
  }
]
```
- **Service:** `ProdutoService.listarProdutosComEstoqueBaixo()`

---

## 5. Categorias (`/categorias`)

### 5.1. Listar Categorias (Paginado)
- **Método:** `GET`
- **Endpoint:** `/categorias/org/{orgId}?page={page}&size={size}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `orgId` (number): ID da organização
- **Query Parameters:**
  - `page` (number, opcional): Número da página (padrão: 0)
  - `size` (number, opcional): Tamanho da página (padrão: 10)
- **Resposta (200 OK):**
```json
{
  "content": [
    {
      "id": number,
      "nome": "string"
    }
  ],
  "totalPages": number,
  "totalElements": number,
  "number": number,
  "size": number
}
```
- **Service:** `CategoriaService.listarCategorias(page, size)`

### 5.2. Buscar Categoria por Nome
- **Método:** `GET`
- **Endpoint:** `/categorias/org/{orgId}/nome/{nome}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `orgId` (number): ID da organização
  - `nome` (string): Nome da categoria
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string"
}
```
- **Resposta (404 Not Found):** Categoria não encontrada

### 5.3. Buscar Categorias por Parte do Nome
- **Método:** `GET`
- **Endpoint:** `/categorias/org/{orgId}/parte-do-nome/{parteDoNome}?page={page}&size={size}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `orgId` (number): ID da organização
  - `parteDoNome` (string): Parte do nome para busca
- **Query Parameters:**
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Mesmo formato da listagem paginada

### 5.4. Criar Categoria
- **Método:** `POST`
- **Endpoint:** `/categorias/org/{orgId}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `orgId` (number): ID da organização
- **Body:**
```json
{
  "nome": "string",
  "descricao": "string"
}
```
- **Resposta (201 Created):**
```json
{
  "id": number,
  "nome": "string"
}
```
- **Service:** `CategoriaService.criarCategoria(nome, descricao)`

### 5.5. Atualizar Categoria
- **Método:** `PUT`
- **Endpoint:** `/categorias/org/{orgId}/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `orgId` (number): ID da organização
  - `id` (number): ID da categoria
- **Body:**
```json
{
  "nome": "string",
  "descricao": "string"
}
```
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string"
}
```
- **Resposta (404 Not Found):** Categoria não encontrada

### 5.6. Deletar Categoria
- **Método:** `DELETE`
- **Endpoint:** `/categorias/org/{orgId}/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `orgId` (number): ID da organização
  - `id` (number): ID da categoria
- **Resposta (204 No Content):** Sucesso
- **Resposta (404 Not Found):** Categoria não encontrada
- **Service:** `CategoriaService.deletarCategoria(id)`

---

## 6. Consumidores (`/consumidores`)

### 6.1. Listar Consumidores (Paginado)
- **Método:** `GET`
- **Endpoint:** `/consumidores?page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `page` (number, opcional): Número da página (padrão: 0)
  - `size` (number, opcional): Tamanho da página (padrão: 10)
- **Resposta (200 OK):**
```json
{
  "content": [
    {
      "id": number,
      "nome": "string",
      "cpf": "string",
      "endereco": "string",
      "org": {
        "id": number,
        "nome": "string"
      }
    }
  ],
  "totalPages": number,
  "totalElements": number,
  "number": number,
  "size": number
}
```
- **Service:** `ConsumidorService.listarConsumidoresPaged(page, size)`

### 6.2. Buscar Consumidor por ID
- **Método:** `GET`
- **Endpoint:** `/consumidores/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do consumidor
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string",
  "cpf": "string",
  "endereco": "string",
  "org": {
    "id": number,
    "nome": "string"
  }
}
```
- **Service:** `ConsumidorService.getById(id)`

### 6.3. Criar Consumidor
- **Método:** `POST`
- **Endpoint:** `/consumidores`
- **Autenticação:** Requerida
- **Body:**
```json
{
  "nome": "string",
  "cpf": "string",
  "endereco": "string",
  "orgId": number
}
```
- **Resposta (201 Created):**
```json
{
  "id": number,
  "nome": "string",
  "cpf": "string",
  "endereco": "string",
  "org": {
    "id": number,
    "nome": "string"
  }
}
```
- **Service:** `ConsumidorService.criarConsumidor(consumidor)`

### 6.4. Atualizar Consumidor
- **Método:** `PUT`
- **Endpoint:** `/consumidores/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do consumidor
- **Body:**
```json
{
  "nome": "string",
  "cpf": "string",
  "endereco": "string",
  "orgId": number
}
```
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string",
  "cpf": "string",
  "endereco": "string",
  "org": {
    "id": number,
    "nome": "string"
  }
}
```
- **Service:** `ConsumidorService.editarConsumidor(consumidor)`

### 6.5. Deletar Consumidor
- **Método:** `DELETE`
- **Endpoint:** `/consumidores/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do consumidor
- **Resposta (200 OK):**
```json
{
  "message": "Consumidor excluído com sucesso"
}
```
- **Service:** `ConsumidorService.deletarConsumidor(id)`

---

## 7. Entregas (`/entregas`)

### 7.1. Listar Entregas (Paginado)
- **Método:** `GET`
- **Endpoint:** `/entregas?page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `page` (number, opcional): Número da página (padrão: 0)
  - `size` (number, opcional): Tamanho da página (padrão: 10)
- **Resposta (200 OK):**
```json
{
  "content": [
    {
      "id": number,
      "dataEntrega": "string (ISO DateTime)",
      "quantidade": number,
      "produto": {
        "id": number,
        "nome": "string"
      },
      "consumidor": {
        "id": number,
        "nome": "string"
      }
    }
  ],
  "totalPages": number,
  "totalElements": number
}
```
- **Service:** `EntregasService.listarEntregas(page, size)`

### 7.2. Criar Entrega
- **Método:** `POST`
- **Endpoint:** `/entregas`
- **Autenticação:** Requerida
- **Body:**
```json
{
  "produtoId": number,
  "consumidorId": number,
  "quantidade": number,
  "dataEntrega": "string (ISO DateTime)"
}
```
- **Resposta (201 Created):**
```json
{
  "id": number,
  "dataEntrega": "string (ISO DateTime)",
  "quantidade": number,
  "produto": {
    "id": number,
    "nome": "string"
  },
  "consumidor": {
    "id": number,
    "nome": "string"
  },
  "aviso": {
    "estoqueBaixo": boolean,
    "mensagem": "string"
  }
}
```
- **Service:** `EntregasService.criarEntrega(entrega)`

### 7.3. Atualizar Entrega
- **Método:** `PUT`
- **Endpoint:** `/entregas/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da entrega
- **Body:**
```json
{
  "produtoId": number,
  "consumidorId": number,
  "quantidade": number,
  "dataEntrega": "string (ISO DateTime)"
}
```
- **Resposta (200 OK):**
```json
{
  "id": number,
  "dataEntrega": "string (ISO DateTime)",
  "quantidade": number,
  "produto": {
    "id": number,
    "nome": "string"
  },
  "consumidor": {
    "id": number,
    "nome": "string"
  }
}
```
- **Service:** `EntregasService.editarEntrega(id, entrega)`

### 7.4. Deletar Entrega
- **Método:** `DELETE`
- **Endpoint:** `/entregas/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da entrega
- **Resposta (204 No Content)**
- **Service:** `EntregasService.deletarEntrega(id)`

### 7.5. Listar Entregas por Dia
- **Método:** `GET`
- **Endpoint:** `/entregas/por-dia?dia={dia}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `dia` (string, obrigatório): Data no formato ISO DATE (YYYY-MM-DD)
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Mesmo formato da listagem paginada
- **Service:** `EntregasService.porDia(dia)`

### 7.6. Listar Entregas por Período
- **Método:** `GET`
- **Endpoint:** `/entregas/por-periodo?inicio={inicio}&fim={fim}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `inicio` (string, obrigatório): Data/hora inicial no formato ISO DATE_TIME (YYYY-MM-DDTHH:MM:SS)
  - `fim` (string, obrigatório): Data/hora final no formato ISO DATE_TIME (YYYY-MM-DDTHH:MM:SS)
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Mesmo formato da listagem paginada
- **Service:** `EntregasService.porPeriodo(inicio, fim)`

### 7.7. Listar Entregas por Mês
- **Método:** `GET`
- **Endpoint:** `/entregas/por-mes?mes={mes}&ano={ano}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `mes` (number, obrigatório): Mês (1-12)
  - `ano` (number, obrigatório): Ano (ex: 2025)
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Mesmo formato da listagem paginada
- **Service:** `EntregasService.porMes(mes, ano)`

### 7.8. Listar Entregas por Ano
- **Método:** `GET`
- **Endpoint:** `/entregas/por-ano?ano={ano}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `ano` (number, obrigatório): Ano (ex: 2025)
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Mesmo formato da listagem paginada
- **Service:** `EntregasService.porAno(ano)`

### 7.9. Listar Entregas por Consumidor
- **Método:** `GET`
- **Endpoint:** `/entregas/por-consumidor/{consumidorId}?page={page}&size={size}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `consumidorId` (number): ID do consumidor
- **Query Parameters:**
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Mesmo formato da listagem paginada
- **Service:** `EntregasService.porConsumidor(consumidorId)`

### 7.10. Listar Entregas por Consumidor e Período
- **Método:** `GET`
- **Endpoint:** `/entregas/por-consumidor/{consumidorId}/periodo?inicio={inicio}&fim={fim}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `consumidorId` (number): ID do consumidor
- **Query Parameters:**
  - `inicio` (string, obrigatório): Data/hora inicial no formato ISO DATE_TIME
  - `fim` (string, obrigatório): Data/hora final no formato ISO DATE_TIME
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Mesmo formato da listagem paginada
- **Service:** `EntregasService.porConsumidorPeriodo(consumidorId, inicio, fim)`

### 7.11. Listar Entregas por Produto
- **Método:** `GET`
- **Endpoint:** `/entregas/por-produto/{produtoId}?orgId={orgId}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `produtoId` (number): ID do produto
- **Query Parameters:**
  - `orgId` (number, obrigatório): ID da organização
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Mesmo formato da listagem paginada
- **Service:** `EntregasService.porProduto(produtoId, orgId, page, size)`

### 7.12. Obter Total de Entregas Realizadas
- **Método:** `GET`
- **Endpoint:** `/entregas/total`
- **Autenticação:** Requerida
- **Resposta (200 OK):**
```json
number
```
- **Service:** `EntregasService.getTotalEntregasRealizadas()`

### 7.13. Obter Total de Entregas por Consumidor
- **Método:** `GET`
- **Endpoint:** `/entregas/total/consumidor/{consumidorId}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `consumidorId` (number): ID do consumidor
- **Resposta (200 OK):**
```json
number
```
- **Service:** `EntregasService.getTotalEntregasPorConsumidor(consumidorId)`

### 7.14. Obter Total de Entregas por Produto
- **Método:** `GET`
- **Endpoint:** `/entregas/total/produto/{produtoId}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `produtoId` (number): ID do produto
- **Resposta (200 OK):**
```json
number
```
- **Service:** `EntregasService.getTotalEntregasPorProduto(produtoId)`

---

## 8. Movimentações de Produto (`/movimentacoes`)

### 8.1. Registrar Movimentação
- **Método:** `POST`
- **Endpoint:** `/movimentacoes`
- **Autenticação:** Requerida
- **Body:**
```json
{
  "produtoId": number,
  "tipo": "ENTRADA" | "SAIDA",
  "quantidade": number,
  "dataMovimentacao": "string (ISO DateTime)",
  "observacao": "string",
  "consumidorId": number
}
```
- **Resposta (200 OK):**
```json
{
  "id": number,
  "produtoId": number,
  "tipo": "ENTRADA" | "SAIDA",
  "quantidade": number,
  "dataMovimentacao": "string (ISO DateTime)",
  "observacao": "string",
  "consumidorId": number,
  "produto": {
    "id": number,
    "nome": "string"
  },
  "consumidor": {
    "id": number,
    "nome": "string"
  }
}
```
- **Service:** `MovimentacaoProdutoService.registrarMovimentacao(dto)`

### 8.2. Editar Movimentação
- **Método:** `PUT`
- **Endpoint:** `/movimentacoes/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da movimentação
- **Body:** Mesmo formato do registro
- **Resposta (200 OK):** Mesmo formato do registro
- **Service:** `MovimentacaoProdutoService.editarMovimentacao(id, dto)`

### 8.3. Listar Movimentações por Produto
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/produto/{produtoId}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `produtoId` (number): ID do produto
- **Resposta (200 OK):**
```json
[
  {
    "id": number,
    "produtoId": number,
    "tipo": "ENTRADA" | "SAIDA",
    "quantidade": number,
    "dataMovimentacao": "string (ISO DateTime)",
    "observacao": "string",
    "consumidorId": number
  }
]
```
- **Service:** `MovimentacaoProdutoService.listarPorProduto(produtoId)`

### 8.4. Buscar Movimentações por Data
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-data?tipo={tipo}&data={data}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `tipo` (string, obrigatório): "ENTRADA" ou "SAIDA"
  - `data` (string, obrigatório): Data no formato ISO DATE (YYYY-MM-DD)
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorData(tipo, data, page, size)`

### 8.5. Buscar Movimentações por Período
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-periodo?tipo={tipo}&inicio={inicio}&fim={fim}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `tipo` (string, obrigatório): "ENTRADA" ou "SAIDA"
  - `inicio` (string, obrigatório): Data/hora inicial no formato ISO DATE_TIME
  - `fim` (string, obrigatório): Data/hora final no formato ISO DATE_TIME
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorPeriodo(tipo, inicio, fim, page, size)`

### 8.6. Buscar Movimentações por Ano
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-ano?ano={ano}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `ano` (number, obrigatório): Ano (ex: 2025)
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorAno(ano, page, size)`

### 8.7. Buscar Movimentações por Mês
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-mes?ano={ano}&mes={mes}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `ano` (number, obrigatório): Ano (ex: 2025)
  - `mes` (number, obrigatório): Mês (1-12)
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorMes(ano, mes, page, size)`

### 8.8. Buscar Movimentações por Nome do Produto
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-nome?nomeProduto={nomeProduto}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `nomeProduto` (string, obrigatório): Nome do produto
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorNomeProduto(nomeProduto, page, size)`

### 8.9. Buscar Movimentações por Categoria do Produto
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-categoria?categoriaProduto={categoriaProduto}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `categoriaProduto` (string, obrigatório): Nome da categoria
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorCategoriaProduto(categoriaProduto, page, size)`

### 8.10. Buscar Movimentações por ID do Produto
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-id?produtoId={produtoId}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `produtoId` (number, obrigatório): ID do produto
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorIdProduto(produtoId, page, size)`

### 8.11. Buscar Movimentações por Intervalo (Filtros Combinados)
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-intervalo?nome={nome}&categoria={categoria}&produtoId={produtoId}&inicio={inicio}&fim={fim}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `nome` (string, opcional): Nome do produto
  - `categoria` (string, opcional): Nome da categoria
  - `produtoId` (number, opcional): ID do produto
  - `inicio` (string, obrigatório): Data/hora inicial no formato ISO DATE_TIME
  - `fim` (string, obrigatório): Data/hora final no formato ISO DATE_TIME
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorProdutoNomeCategoriaIdAndIntervalo(nome, categoria, produtoId, inicio, fim, page, size)`

### 8.12. Buscar Movimentações por Tipos
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-tipos?tipos={tipos}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `tipos` (string, obrigatório): Lista separada por vírgula (ex: "ENTRADA,SAIDA")
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorTipos(tipos, page, size)`

### 8.13. Buscar Movimentações por Consumidor
- **Método:** `GET`
- **Endpoint:** `/movimentacoes/por-consumidor?nome={nome}&page={page}&size={size}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `nome` (string, obrigatório): Nome do consumidor
  - `page` (number, opcional): Número da página
  - `size` (number, opcional): Tamanho da página
- **Resposta (200 OK):** Formato paginado
- **Service:** `MovimentacaoProdutoService.buscarPorConsumidor(nomeConsumidor, page, size)`

### 8.14. Atualizar Consumidor da Movimentação
- **Método:** `PATCH`
- **Endpoint:** `/movimentacoes/{id}/consumidor?consumidorId={consumidorId}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da movimentação
- **Query Parameters:**
  - `consumidorId` (number, obrigatório): ID do consumidor
- **Resposta (200 OK):** Mesmo formato do registro

---

## 9. Planos (`/api/plans`)

### 9.1. Listar Todos os Planos Ativos
- **Método:** `GET`
- **Endpoint:** `/api/plans`
- **Autenticação:** Requerida
- **Resposta (200 OK):**
```json
[
  {
    "id": number,
    "nome": "string",
    "tipo": "BASIC" | "PREMIUM" | "ENTERPRISE",
    "preco": number,
    "ativo": boolean,
    "maxUsuarios": number,
    "maxProdutos": number,
    "maxOrganizacoes": number
  }
]
```
- **Service:** `PlanService.getAllPlans()`

### 9.2. Buscar Plano por ID
- **Método:** `GET`
- **Endpoint:** `/api/plans/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do plano
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string",
  "tipo": "BASIC" | "PREMIUM" | "ENTERPRISE",
  "preco": number,
  "ativo": boolean,
  "maxUsuarios": number,
  "maxProdutos": number,
  "maxOrganizacoes": number
}
```
- **Resposta (404 Not Found):** Plano não encontrado
- **Service:** `PlanService.getPlanById(id)`

### 9.3. Buscar Plano por Tipo
- **Método:** `GET`
- **Endpoint:** `/api/plans/type/{type}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `type` (string): "BASIC", "PREMIUM" ou "ENTERPRISE"
- **Resposta (200 OK):** Mesmo formato do plano por ID
- **Resposta (404 Not Found):** Plano não encontrado

### 9.4. Obter Recursos do Plano
- **Método:** `GET`
- **Endpoint:** `/api/plans/{id}/features`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do plano
- **Resposta (200 OK):**
```json
{
  "maxUsuarios": number,
  "maxProdutos": number,
  "maxOrganizacoes": number,
  "recursos": ["string"]
}
```
- **Resposta (404 Not Found):** Plano não encontrado

### 9.5. Validar Adição de Usuário
- **Método:** `GET`
- **Endpoint:** `/api/plans/{id}/validate-user?currentUserCount={currentUserCount}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do plano
- **Query Parameters:**
  - `currentUserCount` (number, obrigatório): Número atual de usuários
- **Resposta (200 OK):**
```json
{
  "canAdd": boolean,
  "currentCount": number,
  "maxCount": number,
  "message": "string"
}
```

### 9.6. Validar Adição de Produto
- **Método:** `GET`
- **Endpoint:** `/api/plans/{id}/validate-product?currentProductCount={currentProductCount}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do plano
- **Query Parameters:**
  - `currentProductCount` (number, obrigatório): Número atual de produtos
- **Resposta (200 OK):** Mesmo formato da validação de usuário

### 9.7. Validar Adição de Organização
- **Método:** `GET`
- **Endpoint:** `/api/plans/{id}/validate-organization?currentOrgCount={currentOrgCount}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID do plano
- **Query Parameters:**
  - `currentOrgCount` (number, obrigatório): Número atual de organizações
- **Resposta (200 OK):** Mesmo formato da validação de usuário

### 9.8. Sincronizar Planos com Mercado Pago
- **Método:** `POST`
- **Endpoint:** `/api/plans/sync-mercadopago`
- **Autenticação:** Requerida
- **Resposta (200 OK):**
```json
{
  "status": "success",
  "message": "string"
}
```
- **Resposta (500 Internal Server Error):** Em caso de erro

---

## 10. Assinaturas (`/api/subscription`)

### 10.1. Obter Assinatura Atual
- **Método:** `GET`
- **Endpoint:** `/api/subscription/current`
- **Autenticação:** Requerida
- **Resposta (200 OK):**
```json
{
  "id": number,
  "planId": number,
  "status": "string",
  "startDate": "string (ISO DateTime)",
  "endDate": "string (ISO DateTime)",
  "plan": {
    "id": number,
    "nome": "string"
  }
}
```
- **Resposta (404 Not Found):** Sem assinatura ativa

### 10.2. Criar Assinatura (redirect para Mercado Pago)
- **Método:** `POST`
- **Endpoint:** `/api/subscription/create?planId={planId}` ou `/api/subscription/checkout?planId={planId}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `planId` (obrigatório): ID do plano
- **Resposta (200 OK):**
```json
{
  "subscriptionId": number,
  "checkoutUrl": "string",
  "status": "string"
}
```

### 10.3. Checkout Transparente (pagamento no próprio site)
- **Método:** `POST`
- **Endpoint:** `/api/subscription/checkout`
- **Autenticação:** Requerida (Bearer JWT)
- **Body (JSON):** Dados obrigatórios para o checkout com cartão tokenizado:

| Campo               | Tipo   | Obrigatório | Descrição |
|---------------------|--------|-------------|-----------|
| `planId`            | string | Sim         | ID do plano. |
| `cardTokenId`       | string | Sim         | Token do cartão (retorno de `cardForm.getCardFormData().token`). |
| `payerEmail`        | string | Sim         | E-mail do pagador. |
| `externalReference` | string | Sim         | ID do usuário logado (obtido via `GET /api/subscription/current-user-id`). |

Exemplo de body:
```json
{
  "planId": "1",
  "cardTokenId": "ff8080814a11b4b9014a11c2f3ab0001",
  "payerEmail": "comprador@email.com",
  "externalReference": "123"
}
```

- **Resposta (200 OK)** quando checkout transparente for usado com sucesso:
```json
{
  "checkoutUrl": null,
  "sessionId": null,
  "subscriptionId": number,
  "status": "string",
  "transparentCheckout": true,
  "preapprovalId": "string"
}
```
- O frontend não deve redirecionar para URL; deve exibir sucesso e ir para `/subscription/success`.

### 10.4. Cancelar Assinatura
- **Método:** `POST`
- **Endpoint:** `/api/subscription/cancel`
- **Autenticação:** Requerida
- **Resposta (200 OK):**
```json
{
  "status": "success",
  "message": "string"
}
```

### 10.5. Obter Portal do Cliente
- **Método:** `GET`
- **Endpoint:** `/api/subscription/customer-portal`
- **Autenticação:** Requerida
- **Resposta (200 OK):**
```json
{
  "url": "string"
}
```

### 10.6. Obter Histórico de Assinaturas
- **Método:** `GET`
- **Endpoint:** `/api/subscription/history`
- **Autenticação:** Requerida
- **Resposta (200 OK):**
```json
[
  {
    "id": number,
    "planId": number,
    "status": "string",
    "startDate": "string (ISO DateTime)",
    "endDate": "string (ISO DateTime)"
  }
]
```

### 10.7. Verificar Acesso a Funcionalidade
- **Método:** `GET`
- **Endpoint:** `/api/subscription/feature-access?feature={feature}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `feature` (string, obrigatório): Nome da funcionalidade
- **Resposta (200 OK):**
```json
{
  "hasAccess": boolean
}
```

### 10.8. Verificar Limites de Uso
- **Método:** `GET`
- **Endpoint:** `/api/subscription/usage-limits?limitType={limitType}&currentCount={currentCount}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `limitType` (string, obrigatório): Tipo de limite ("users", "products", "organizations")
  - `currentCount` (number, obrigatório): Contagem atual
- **Resposta (200 OK):**
```json
{
  "canAdd": boolean,
  "currentCount": number,
  "maxCount": number,
  "message": "string"
}
```

### 10.9. Listar Todas as Assinaturas (Admin)
- **Método:** `GET`
- **Endpoint:** `/api/subscription/all`
- **Autenticação:** Requerida (Role: ADMIN)
- **Resposta (200 OK):** Lista de todas as assinaturas

---

## 11. Relatórios (`/relatorios`)

Todos os endpoints de relatórios retornam arquivos binários (PDF ou XLSX).

### 11.1. Relatório de Estoque Baixo (PDF)
- **Método:** `GET`
- **Endpoint:** `/relatorios/estoque-baixo.pdf`
- **Autenticação:** Requerida
- **Headers:**
  - `Accept: application/pdf`
- **Resposta (200 OK):** Arquivo PDF (blob)
- **Service:** `RelatoriosService.estoqueBaixoPdf()`

### 11.2. Relatório de Estoque Baixo (XLSX)
- **Método:** `GET`
- **Endpoint:** `/relatorios/estoque-baixo.xlsx`
- **Autenticação:** Requerida
- **Headers:**
  - `Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Resposta (200 OK):** Arquivo XLSX (blob)
- **Service:** `RelatoriosService.estoqueBaixoXlsx()`

### 11.3. Relatório de Entregas por Período (PDF)
- **Método:** `GET`
- **Endpoint:** `/relatorios/entregas-periodo.pdf?inicio={inicio}&fim={fim}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `inicio` (string, obrigatório): Data/hora inicial no formato ISO DATE_TIME com timezone (OffsetDateTime)
  - `fim` (string, obrigatório): Data/hora final no formato ISO DATE_TIME com timezone (OffsetDateTime)
- **Headers:**
  - `Accept: application/pdf`
- **Resposta (200 OK):** Arquivo PDF (blob)
- **Service:** `RelatoriosService.entregasPeriodoPdf(inicioISO, fimISO)`

### 11.4. Relatório de Entregas por Período (XLSX)
- **Método:** `GET`
- **Endpoint:** `/relatorios/entregas-periodo.xlsx?inicio={inicio}&fim={fim}`
- **Autenticação:** Requerida
- **Query Parameters:** Mesmos do PDF
- **Headers:**
  - `Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Resposta (200 OK):** Arquivo XLSX (blob)
- **Service:** `RelatoriosService.entregasPeriodoXlsx(inicioISO, fimISO)`

### 11.5. Relatório de Entregas do Dia (PDF)
- **Método:** `GET`
- **Endpoint:** `/relatorios/entregas-dia.pdf?dia={dia}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `dia` (string, obrigatório): Data no formato ISO DATE (YYYY-MM-DD)
- **Headers:**
  - `Accept: application/pdf`
- **Resposta (200 OK):** Arquivo PDF (blob)
- **Service:** `RelatoriosService.entregasDiaPdf(diaISO)`

### 11.6. Relatório de Entregas do Mês (PDF)
- **Método:** `GET`
- **Endpoint:** `/relatorios/entregas-mes.pdf?ano={ano}&mes={mes}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `ano` (number, obrigatório): Ano (ex: 2025)
  - `mes` (number, obrigatório): Mês (1-12)
- **Headers:**
  - `Accept: application/pdf`
- **Resposta (200 OK):** Arquivo PDF (blob)
- **Service:** `RelatoriosService.entregasMesPdf(ano, mes)`

### 11.7. Relatório de Entregas do Mês (XLSX)
- **Método:** `GET`
- **Endpoint:** `/relatorios/entregas-mes.xlsx?ano={ano}&mes={mes}`
- **Autenticação:** Requerida
- **Query Parameters:** Mesmos do PDF
- **Headers:**
  - `Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Resposta (200 OK):** Arquivo XLSX (blob)
- **Service:** `RelatoriosService.entregasMesXlsx(ano, mes)`

### 11.8. Relatório de Entregas do Ano (PDF)
- **Método:** `GET`
- **Endpoint:** `/relatorios/entregas-ano.pdf?ano={ano}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `ano` (number, obrigatório): Ano (ex: 2025)
- **Headers:**
  - `Accept: application/pdf`
- **Resposta (200 OK):** Arquivo PDF (blob)
- **Service:** `RelatoriosService.entregasAnoPdf(ano)`

### 11.9. Relatório de Entregas do Ano (XLSX)
- **Método:** `GET`
- **Endpoint:** `/relatorios/entregas-ano.xlsx?ano={ano}`
- **Autenticação:** Requerida
- **Query Parameters:** Mesmos do PDF
- **Headers:**
  - `Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Resposta (200 OK):** Arquivo XLSX (blob)
- **Service:** `RelatoriosService.entregasAnoXlsx(ano)`

### 11.10. Relatório de Movimentações do Mês (PDF)
- **Método:** `GET`
- **Endpoint:** `/relatorios/movimentacoes-mes.pdf?ano={ano}&mes={mes}`
- **Autenticação:** Requerida
- **Query Parameters:**
  - `ano` (number, obrigatório): Ano (ex: 2025)
  - `mes` (number, obrigatório): Mês (1-12)
- **Headers:**
  - `Accept: application/pdf`
- **Resposta (200 OK):** Arquivo PDF (blob)
- **Service:** `RelatoriosService.movimentacoesMesPdf(ano, mes)`

### 11.11. Relatório de Movimentações do Mês (XLSX)
- **Método:** `GET`
- **Endpoint:** `/relatorios/movimentacoes-mes.xlsx?ano={ano}&mes={mes}`
- **Autenticação:** Requerida
- **Query Parameters:** Mesmos do PDF
- **Headers:**
  - `Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Resposta (200 OK):** Arquivo XLSX (blob)
- **Service:** `RelatoriosService.movimentacoesMesXlsx(ano, mes)`

---

## 12. Roles (`/roles`)

### 12.1. Listar Todas as Roles
- **Método:** `GET`
- **Endpoint:** `/roles`
- **Autenticação:** Requerida
- **Resposta (200 OK):**
```json
[
  {
    "id": number,
    "nome": "string"
  }
]
```
- **Resposta (204 No Content):** Quando não há roles
- **Service:** `RoleService.listarRoles()`

### 12.2. Buscar Role por ID
- **Método:** `GET`
- **Endpoint:** `/roles/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da role
- **Resposta (200 OK):**
```json
{
  "id": number,
  "nome": "string"
}
```
- **Resposta (404 Not Found):** Role não encontrada

### 12.3. Criar Role
- **Método:** `POST`
- **Endpoint:** `/roles`
- **Autenticação:** Requerida
- **Body:**
```json
{
  "nome": "string"
}
```
- **Resposta (201 Created):**
```json
{
  "id": number,
  "nome": "string"
}
```
- **Service:** `RoleService.criarRole(role)`

### 12.4. Atualizar Role
- **Método:** `PUT`
- **Endpoint:** `/roles/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da role
- **Body:**
```json
{
  "nome": "string"
}
```
- **Resposta (200 OK):** Mesmo formato da criação
- **Resposta (404 Not Found):** Role não encontrada

### 12.5. Deletar Role
- **Método:** `DELETE`
- **Endpoint:** `/roles/{id}`
- **Autenticação:** Requerida
- **Parâmetros Path:**
  - `id` (number): ID da role
- **Resposta (204 No Content):** Sucesso
- **Resposta (404 Not Found):** Role não encontrada
- **Service:** `RoleService.deletarRole(id)`

---

## 13. Cache (`/api/cache`)

**Nota:** Todos os endpoints de cache requerem autenticação com role ADMIN.

### 13.1. Obter Informações do Cache
- **Método:** `GET`
- **Endpoint:** `/api/cache/info`
- **Autenticação:** Requerida (Role: ADMIN)
- **Resposta (200 OK):**
```json
{
  "caches": [
    {
      "name": "string",
      "size": number
    }
  ]
}
```
- **Resposta (404 Not Found):** Cache não disponível

### 13.2. Obter Estatísticas do Cache
- **Método:** `GET`
- **Endpoint:** `/api/cache/stats`
- **Autenticação:** Requerida (Role: ADMIN)
- **Resposta (200 OK):**
```json
{
  "totalCaches": number,
  "totalSize": number,
  "stats": {}
}
```
- **Resposta (404 Not Found):** Cache não disponível

### 13.3. Limpar Cache Específico
- **Método:** `DELETE`
- **Endpoint:** `/api/cache/{cacheName}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Parâmetros Path:**
  - `cacheName` (string): Nome do cache
- **Resposta (200 OK):**
```json
"Cache '{cacheName}' limpo com sucesso"
```
- **Resposta (404 Not Found):** Cache não encontrado

### 13.4. Limpar Todos os Caches
- **Método:** `DELETE`
- **Endpoint:** `/api/cache/all`
- **Autenticação:** Requerida (Role: ADMIN)
- **Resposta (200 OK):**
```json
"Todos os caches foram limpos com sucesso"
```
- **Resposta (404 Not Found):** Cache não disponível

### 13.5. Remover Chave Específica do Cache
- **Método:** `DELETE`
- **Endpoint:** `/api/cache/{cacheName}/keys/{key}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Parâmetros Path:**
  - `cacheName` (string): Nome do cache
  - `key` (string): Chave a ser removida
- **Resposta (200 OK):**
```json
"Chave '{key}' removida do cache '{cacheName}'"
```
- **Resposta (404 Not Found):** Cache ou chave não encontrada

### 13.6. Verificar Existência de Chave no Cache
- **Método:** `GET`
- **Endpoint:** `/api/cache/{cacheName}/keys/{key}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Parâmetros Path:**
  - `cacheName` (string): Nome do cache
  - `key` (string): Chave a ser verificada
- **Resposta (200 OK):**
```json
{
  "cacheName": "string",
  "key": "string",
  "exists": boolean
}
```
- **Resposta (404 Not Found):** Cache não disponível

---

## 14. Webhooks (`/webhooks`)

### 14.1. Receber Webhook do Mercado Pago
- **Método:** `POST`
- **Endpoint:** `/webhooks/mercadopago`
- **Autenticação:** Não requerida (endpoint público)
- **Headers:**
  - `x-signature` (string, opcional): Assinatura do webhook
  - `x-request-id` (string, opcional): ID da requisição
- **Body:** String JSON raw do webhook
- **Resposta (200 OK):**
```json
{
  "status": "success",
  "eventId": "string",
  "eventType": "string"
}
```
- **Resposta (401 Unauthorized):** Assinatura inválida
- **Resposta (400 Bad Request):** Payload inválido

### 14.2. Receber Webhook do Stripe (Deprecated)
- **Método:** `POST`
- **Endpoint:** `/webhooks/stripe`
- **Autenticação:** Não requerida
- **Nota:** Este endpoint está deprecated e redireciona para `/webhooks/mercadopago`
- **Resposta:** Mesma do endpoint de Mercado Pago

---

## 15. Admin - Failed Webhook Events (`/admin/webhooks/failed`)

**Nota:** Todos os endpoints desta seção requerem autenticação com role ADMIN.

### 15.1. Listar Todos os Eventos Falhados
- **Método:** `GET`
- **Endpoint:** `/admin/webhooks/failed`
- **Autenticação:** Requerida (Role: ADMIN)
- **Resposta (200 OK):**
```json
[
  {
    "id": number,
    "eventId": "string",
    "eventType": "string",
    "payload": "string",
    "errorMessage": "string",
    "retryCount": number,
    "createdAt": "string (ISO DateTime)"
  }
]
```

### 15.2. Listar Eventos Falhados por Tipo
- **Método:** `GET`
- **Endpoint:** `/admin/webhooks/failed/type/{eventType}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Parâmetros Path:**
  - `eventType` (string): Tipo do evento
- **Resposta (200 OK):** Lista de eventos falhados do tipo especificado

### 15.3. Buscar Evento Falhado por Event ID
- **Método:** `GET`
- **Endpoint:** `/admin/webhooks/failed/{eventId}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Parâmetros Path:**
  - `eventId` (string): ID do evento
- **Resposta (200 OK):** Detalhes do evento falhado
- **Resposta (404 Not Found):** Evento não encontrado

### 15.4. Reprocessar Evento Falhado
- **Método:** `POST`
- **Endpoint:** `/admin/webhooks/failed/{eventId}/reprocess`
- **Autenticação:** Requerida (Role: ADMIN)
- **Parâmetros Path:**
  - `eventId` (string): ID do evento
- **Resposta (200 OK):**
```json
{
  "status": "success",
  "message": "Evento reprocessado com sucesso",
  "eventId": "string",
  "retryCount": number
}
```
- **Resposta (404 Not Found):** Evento não encontrado
- **Resposta (500 Internal Server Error):** Erro ao reprocessar

### 15.5. Reprocessar Múltiplos Eventos (Batch)
- **Método:** `POST`
- **Endpoint:** `/admin/webhooks/failed/reprocess/batch?eventType={eventType}`
- **Autenticação:** Requerida (Role: ADMIN)
- **Query Parameters:**
  - `eventType` (string, opcional): Filtrar por tipo de evento
- **Resposta (200 OK):**
```json
{
  "status": "completed",
  "total": number,
  "success": number,
  "errors": number
}
```

---

## Observações Importantes

### Autenticação
- A maioria dos endpoints requer autenticação via JWT token
- O token deve ser enviado no header: `Authorization: Bearer {token}`
- O token é obtido através do endpoint `/auth/login`
- O token contém informações do usuário, incluindo `orgId` que é usado automaticamente em muitos endpoints

### Paginação
- Muitos endpoints suportam paginação através dos parâmetros `page` e `size`
- Padrão: `page=0` e `size=10` ou `size=20`
- Respostas paginadas seguem o formato:
```json
{
  "content": [...],
  "totalPages": number,
  "totalElements": number,
  "number": number,
  "size": number
}
```

### Formatos de Data
- **ISO DATE:** `YYYY-MM-DD` (ex: `2025-01-15`)
- **ISO DATE_TIME:** `YYYY-MM-DDTHH:MM:SS` (ex: `2025-01-15T10:30:00`)
- **ISO DATE_TIME com Timezone (OffsetDateTime):** `YYYY-MM-DDTHH:MM:SS+HH:MM` (ex: `2025-01-15T10:30:00-03:00`)

### Códigos de Status HTTP
- **200 OK:** Requisição bem-sucedida
- **201 Created:** Recurso criado com sucesso
- **204 No Content:** Operação bem-sucedida sem conteúdo de resposta
- **400 Bad Request:** Requisição inválida
- **401 Unauthorized:** Não autenticado ou token inválido
- **403 Forbidden:** Sem permissão (role insuficiente)
- **404 Not Found:** Recurso não encontrado
- **409 Conflict:** Conflito (ex: nome duplicado)
- **500 Internal Server Error:** Erro interno do servidor

### Tratamento de Erros
- Todos os serviços do frontend devem tratar erros adequadamente
- Erros de autenticação (401) devem redirecionar para login
- Erros de permissão (403) devem exibir mensagem apropriada
- Erros de validação (400) devem exibir mensagens específicas ao usuário

---

## Checklist de Verificação

Use este checklist para verificar se todas as integrações estão corretas:

### Autenticação
- [ ] Login funciona corretamente
- [ ] Token é armazenado no localStorage
- [ ] Token é enviado em todas as requisições autenticadas
- [ ] Logout limpa o token corretamente

### Organizações
- [ ] Listar organizações
- [ ] Criar organização
- [ ] Atualizar organização
- [ ] Ativar/Desativar organização

### Produtos
- [ ] Listar produtos (paginado)
- [ ] Criar produto
- [ ] Atualizar produto
- [ ] Deletar produto
- [ ] Listar produtos com estoque baixo

### Categorias
- [ ] Listar categorias (paginado)
- [ ] Criar categoria
- [ ] Atualizar categoria
- [ ] Deletar categoria

### Consumidores
- [ ] Listar consumidores (paginado)
- [ ] Criar consumidor
- [ ] Atualizar consumidor
- [ ] Deletar consumidor

### Entregas
- [ ] Listar entregas (paginado)
- [ ] Criar entrega
- [ ] Atualizar entrega
- [ ] Deletar entrega
- [ ] Filtros por dia, período, mês, ano
- [ ] Filtros por consumidor e produto
- [ ] Totais de entregas

### Movimentações
- [ ] Registrar movimentação
- [ ] Editar movimentação
- [ ] Listar por produto
- [ ] Filtros por data, período, mês, ano
- [ ] Filtros por nome, categoria, tipo
- [ ] Atualizar consumidor da movimentação

### Planos e Assinaturas
- [ ] Listar planos
- [ ] Obter assinatura atual
- [ ] Criar assinatura
- [ ] Cancelar assinatura
- [ ] Verificar acesso a funcionalidades

### Relatórios
- [ ] Estoque baixo (PDF e XLSX)
- [ ] Entregas por período (PDF e XLSX)
- [ ] Entregas do dia (PDF)
- [ ] Entregas do mês/ano (PDF e XLSX)
- [ ] Movimentações do mês (PDF e XLSX)

### Roles
- [ ] Listar roles
- [ ] Criar role
- [ ] Deletar role

---

**Última atualização:** Janeiro 2025
**Versão da API:** 1.0







