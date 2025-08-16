-- Script para adicionar colunas faltantes na tabela movimentacoes_produto
-- Migração V6: Adicionar campos usuario_id e entrega_id

-- Adicionar coluna usuario_id
ALTER TABLE movimentacoes_produto 
ADD COLUMN usuario_id BIGINT;

-- Adicionar coluna entrega_id
ALTER TABLE movimentacoes_produto 
ADD COLUMN entrega_id BIGINT;

-- Adicionar constraint de foreign key para usuario_id
ALTER TABLE movimentacoes_produto 
ADD CONSTRAINT fk_movimentacao_usuario 
FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

-- Adicionar constraint de foreign key para entrega_id
ALTER TABLE movimentacoes_produto 
ADD CONSTRAINT fk_movimentacao_entrega 
FOREIGN KEY (entrega_id) REFERENCES entrega(id);

-- Comentários:
-- usuario_id: Referência ao usuário que criou/editou a movimentação
-- entrega_id: Referência à entrega associada (se aplicável)
-- Ambas as colunas permitem NULL para compatibilidade com dados existentes