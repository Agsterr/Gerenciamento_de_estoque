-- Adicionar coluna org_id às tabelas existentes
-- Esta migração adiciona a referência à organização nas tabelas que precisam

-- Adicionar org_id à tabela produtos
ALTER TABLE produtos ADD COLUMN org_id BIGINT;
ALTER TABLE produtos ADD CONSTRAINT fk_produtos_org FOREIGN KEY (org_id) REFERENCES orgs(id);

-- Adicionar org_id à tabela categorias
ALTER TABLE categorias ADD COLUMN org_id BIGINT;
ALTER TABLE categorias ADD CONSTRAINT fk_categorias_org FOREIGN KEY (org_id) REFERENCES orgs(id);

-- Adicionar org_id à tabela consumidor
ALTER TABLE consumidor ADD COLUMN org_id BIGINT;
ALTER TABLE consumidor ADD CONSTRAINT fk_consumidor_org FOREIGN KEY (org_id) REFERENCES orgs(id);

-- Adicionar org_id à tabela usuarios
ALTER TABLE usuarios ADD COLUMN org_id BIGINT;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_org FOREIGN KEY (org_id) REFERENCES orgs(id);

-- Adicionar org_id à tabela entrega
ALTER TABLE entrega ADD COLUMN org_id BIGINT;
ALTER TABLE entrega ADD CONSTRAINT fk_entrega_org FOREIGN KEY (org_id) REFERENCES orgs(id);

-- Índices para melhorar performance
CREATE INDEX idx_produtos_org_id ON produtos(org_id);
CREATE INDEX idx_categorias_org_id ON categorias(org_id);
CREATE INDEX idx_consumidor_org_id ON consumidor(org_id);
CREATE INDEX idx_usuarios_org_id ON usuarios(org_id);
CREATE INDEX idx_entrega_org_id ON entrega(org_id);