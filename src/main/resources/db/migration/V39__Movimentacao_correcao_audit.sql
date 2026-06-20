-- Campos para correção de movimentações com rastreabilidade
ALTER TABLE movimentacoes_produto ADD COLUMN IF NOT EXISTS observacao TEXT;
ALTER TABLE movimentacoes_produto ADD COLUMN IF NOT EXISTS movimentacao_origem_id BIGINT REFERENCES movimentacoes_produto(id);

CREATE INDEX IF NOT EXISTS idx_movimentacoes_origem ON movimentacoes_produto(movimentacao_origem_id);
