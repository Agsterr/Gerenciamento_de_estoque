-- Pedidos de venda (saída de estoque para clientes)
CREATE TABLE IF NOT EXISTS pedidos_venda (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(30) NOT NULL,
    consumidor_id BIGINT NOT NULL REFERENCES consumidor(id),
    vendedor_id BIGINT NOT NULL REFERENCES usuarios(id),
    data_hora TIMESTAMP NOT NULL DEFAULT NOW(),
    forma_pagamento VARCHAR(30) NOT NULL,
    condicao_pagamento VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',
    observacao TEXT,
    valor_total DECIMAL(12,2) DEFAULT 0,
    org_id BIGINT NOT NULL REFERENCES orgs(id),
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (numero, org_id)
);

CREATE TABLE IF NOT EXISTS pedido_venda_itens (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedidos_venda(id) ON DELETE CASCADE,
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    quantidade INTEGER NOT NULL,
    preco_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL
);

ALTER TABLE movimentacoes_produto
    ADD COLUMN IF NOT EXISTS pedido_venda_id BIGINT REFERENCES pedidos_venda(id);

CREATE INDEX IF NOT EXISTS idx_pedidos_venda_org_status ON pedidos_venda(org_id, status);
CREATE INDEX IF NOT EXISTS idx_pedidos_venda_data ON pedidos_venda(org_id, data_hora);
CREATE INDEX IF NOT EXISTS idx_mov_pedido_venda ON movimentacoes_produto(pedido_venda_id);
