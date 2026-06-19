-- Categorias são por organização (org_id + nome). A constraint global em nome sozinha
-- impede que orgs diferentes usem o mesmo nome de categoria.
ALTER TABLE categorias DROP CONSTRAINT IF EXISTS categorias_nome_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_categorias_nome_org_id ON categorias (nome, org_id);
