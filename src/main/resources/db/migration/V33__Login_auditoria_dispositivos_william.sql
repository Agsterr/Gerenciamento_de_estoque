-- Auditoria de logins e scaffold de dispositivos (Fase 1/2)
-- Permite bypass de assinatura para usuários de teste além do SUPER_ADMIN

DROP INDEX IF EXISTS uniq_super_admin_bypass_true;

COMMENT ON COLUMN usuarios.bypass_subscription IS
    'Quando TRUE, usuário acessa sem assinatura ativa (SUPER_ADMIN ou usuário de teste via migration/seed).';

CREATE TABLE IF NOT EXISTS acessos_login (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    org_id      BIGINT REFERENCES orgs(id) ON DELETE SET NULL,
    username    VARCHAR(100) NOT NULL,
    ip          VARCHAR(45),
    user_agent  TEXT,
    data_hora   TIMESTAMP NOT NULL DEFAULT NOW(),
    sucesso     BOOLEAN NOT NULL DEFAULT TRUE,
    detalhes    VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_acessos_login_data_hora ON acessos_login (data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_acessos_login_usuario_id ON acessos_login (usuario_id);
CREATE INDEX IF NOT EXISTS idx_acessos_login_username ON acessos_login (username);

COMMENT ON TABLE acessos_login IS 'Registro de tentativas de login (sucesso e falha) para painel master.';

CREATE TABLE IF NOT EXISTS dispositivos_usuario (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    org_id              BIGINT NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
    fingerprint         VARCHAR(128) NOT NULL,
    user_agent          TEXT,
    nome_dispositivo    VARCHAR(150),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    solicitado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    revisado_em         TIMESTAMP,
    revisado_por_id     BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    CONSTRAINT chk_dispositivo_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT uq_dispositivo_usuario_fingerprint UNIQUE (usuario_id, fingerprint)
);

CREATE INDEX IF NOT EXISTS idx_dispositivos_usuario_status ON dispositivos_usuario (status);
CREATE INDEX IF NOT EXISTS idx_dispositivos_usuario_org ON dispositivos_usuario (org_id);

COMMENT ON TABLE dispositivos_usuario IS 'Scaffold Fase 2: controle de acesso por dispositivo (aprovação pelo master).';
