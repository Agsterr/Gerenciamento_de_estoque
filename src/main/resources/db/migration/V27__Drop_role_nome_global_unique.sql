-- Roles são por organização (org_id + nome). A constraint global em nome sozinha
-- impede cadastro de novas orgs (ex.: ROLE_USER já existe na org 1).
ALTER TABLE role DROP CONSTRAINT IF EXISTS role_nome_key;
