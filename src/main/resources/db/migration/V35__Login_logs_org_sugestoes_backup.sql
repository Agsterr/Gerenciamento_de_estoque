-- Índices para consultas de login por organização e data
CREATE INDEX IF NOT EXISTS idx_acessos_login_org_id ON acessos_login (org_id);
CREATE INDEX IF NOT EXISTS idx_acessos_login_org_data_hora ON acessos_login (org_id, data_hora DESC);

-- Caixa de sugestões por tenant
CREATE TABLE IF NOT EXISTS sugestoes (
    id          BIGSERIAL PRIMARY KEY,
    org_id      BIGINT NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
    usuario_id  BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    username    VARCHAR(100) NOT NULL,
    texto       TEXT NOT NULL,
    criado_em   TIMESTAMP NOT NULL DEFAULT NOW(),
    status      VARCHAR(20) NOT NULL DEFAULT 'NOVA',
    CONSTRAINT chk_sugestao_status CHECK (status IN ('NOVA', 'LIDA', 'ARQUIVADA'))
);

CREATE INDEX IF NOT EXISTS idx_sugestoes_org_id ON sugestoes (org_id);
CREATE INDEX IF NOT EXISTS idx_sugestoes_criado_em ON sugestoes (criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_sugestoes_status ON sugestoes (status);

COMMENT ON TABLE sugestoes IS 'Sugestões enviadas por usuários autenticados, vinculadas à organização.';
