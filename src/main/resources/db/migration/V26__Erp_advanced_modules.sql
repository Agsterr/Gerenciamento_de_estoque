-- SKU, código de barras e custo médio em produtos
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS sku VARCHAR(50);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS codigo_barras VARCHAR(50);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS custo_medio DECIMAL(12,2) DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS idx_produtos_sku_org ON produtos(sku, org_id) WHERE sku IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_produtos_codigo_barras_org ON produtos(codigo_barras, org_id) WHERE codigo_barras IS NOT NULL;

-- Fornecedores
CREATE TABLE IF NOT EXISTS fornecedores (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cnpj VARCHAR(18),
    email VARCHAR(150),
    telefone VARCHAR(30),
    endereco TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    org_id BIGINT NOT NULL REFERENCES orgs(id),
    criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Depósitos / armazéns
CREATE TABLE IF NOT EXISTS depositos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    endereco TEXT,
    padrao BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    org_id BIGINT NOT NULL REFERENCES orgs(id),
    criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Estoque por depósito
CREATE TABLE IF NOT EXISTS estoque_deposito (
    id BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    deposito_id BIGINT NOT NULL REFERENCES depositos(id),
    quantidade INTEGER NOT NULL DEFAULT 0,
    org_id BIGINT NOT NULL REFERENCES orgs(id),
    UNIQUE(produto_id, deposito_id)
);

-- Pedidos de compra
CREATE TABLE IF NOT EXISTS pedidos_compra (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(30) NOT NULL,
    fornecedor_id BIGINT NOT NULL REFERENCES fornecedores(id),
    deposito_id BIGINT REFERENCES depositos(id),
    status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',
    observacao TEXT,
    valor_total DECIMAL(12,2) DEFAULT 0,
    org_id BIGINT NOT NULL REFERENCES orgs(id),
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pedido_compra_itens (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedidos_compra(id) ON DELETE CASCADE,
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    quantidade INTEGER NOT NULL,
    preco_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL
);

-- Contagem de inventário (cíclica)
CREATE TABLE IF NOT EXISTS contagens_inventario (
    id BIGSERIAL PRIMARY KEY,
    deposito_id BIGINT NOT NULL REFERENCES depositos(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    observacao TEXT,
    org_id BIGINT NOT NULL REFERENCES orgs(id),
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    finalizado_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contagem_inventario_itens (
    id BIGSERIAL PRIMARY KEY,
    contagem_id BIGINT NOT NULL REFERENCES contagens_inventario(id) ON DELETE CASCADE,
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    quantidade_sistema INTEGER NOT NULL,
    quantidade_contada INTEGER,
    diferenca INTEGER DEFAULT 0
);

-- Auditoria de alterações
CREATE TABLE IF NOT EXISTS auditoria_log (
    id BIGSERIAL PRIMARY KEY,
    entidade VARCHAR(50) NOT NULL,
    entidade_id BIGINT,
    acao VARCHAR(20) NOT NULL,
    usuario VARCHAR(100),
    detalhes TEXT,
    org_id BIGINT NOT NULL REFERENCES orgs(id),
    criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_auditoria_org ON auditoria_log(org_id, criado_em DESC);
