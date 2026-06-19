ALTER TABLE pedidos_venda ADD COLUMN IF NOT EXISTS deposito_id BIGINT REFERENCES depositos(id);
ALTER TABLE movimentacoes_produto ADD COLUMN IF NOT EXISTS deposito_id BIGINT REFERENCES depositos(id);
