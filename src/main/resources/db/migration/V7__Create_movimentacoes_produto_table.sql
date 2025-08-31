-- Criação da tabela movimentacoes_produto
-- Esta tabela armazena todas as movimentações de produtos (entrada/saída)

CREATE TABLE movimentacoes_produto (
    id BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    quantidade INTEGER NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    org_id BIGINT NOT NULL,
    entrega_id BIGINT,
    usuario_id BIGINT,
    consumidor_id BIGINT,
    
    -- Foreign keys
    CONSTRAINT fk_movimentacao_produto FOREIGN KEY (produto_id) REFERENCES produtos(id),
    CONSTRAINT fk_movimentacao_org FOREIGN KEY (org_id) REFERENCES orgs(id),
    CONSTRAINT fk_movimentacao_entrega FOREIGN KEY (entrega_id) REFERENCES entrega(id),
    CONSTRAINT fk_movimentacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_movimentacao_consumidor FOREIGN KEY (consumidor_id) REFERENCES consumidor(id)
);

-- Índices para melhorar performance
CREATE INDEX idx_movimentacoes_produto_id ON movimentacoes_produto(produto_id);
CREATE INDEX idx_movimentacoes_org_id ON movimentacoes_produto(org_id);
CREATE INDEX idx_movimentacoes_data_hora ON movimentacoes_produto(data_hora);
CREATE INDEX idx_movimentacoes_tipo ON movimentacoes_produto(tipo);