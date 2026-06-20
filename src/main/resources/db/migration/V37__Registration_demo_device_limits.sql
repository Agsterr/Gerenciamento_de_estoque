-- Desabilitar cadastro público (flag app), org demo efêmera e limite de dispositivos por org

ALTER TABLE orgs ADD COLUMN IF NOT EXISTS max_dispositivos INTEGER NOT NULL DEFAULT 3;
ALTER TABLE orgs ADD COLUMN IF NOT EXISTS ephemeral BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orgs ADD COLUMN IF NOT EXISTS demo_last_access TIMESTAMP;

COMMENT ON COLUMN orgs.max_dispositivos IS 'Máximo de dispositivos aprovados por usuário na organização (0 = ilimitado).';
COMMENT ON COLUMN orgs.ephemeral IS 'Organização de demonstração — dados operacionais são apagados no logout ou por job.';
COMMENT ON COLUMN orgs.demo_last_access IS 'Último acesso à org demo (para limpeza de sessões abandonadas).';

-- Org demo compartilhada (credenciais: demo / demo123 — senha definida no InitialDataLoader)
INSERT INTO orgs (nome, ativo, max_dispositivos, ephemeral)
SELECT 'org_demo', TRUE, 999, TRUE
WHERE NOT EXISTS (SELECT 1 FROM orgs WHERE nome = 'org_demo');
