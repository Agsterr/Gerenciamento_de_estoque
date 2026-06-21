-- Limite de usuários por organização (definido pelo master; NULL = usa limite do plano)

ALTER TABLE orgs ADD COLUMN IF NOT EXISTS max_usuarios INTEGER;

COMMENT ON COLUMN orgs.max_usuarios IS 'Máximo de usuários ativos na organização. NULL = limite do plano; 0 = ilimitado na org.';
