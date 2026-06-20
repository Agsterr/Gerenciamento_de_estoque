-- Usuários ilimitados (bypass assinatura) e pesquisa de preço

-- Garante bypass para contas conhecidas (criação via InitialDataLoader se ausentes)
UPDATE usuarios
SET bypass_subscription = TRUE
WHERE LOWER(username) IN ('samuel', 'william', 'talison', 'pauloeduardo');

COMMENT ON COLUMN usuarios.bypass_subscription IS
    'Quando TRUE, usuário acessa sem assinatura ativa (SUPER_ADMIN ou conta isenta via admin/migration).';

CREATE TABLE IF NOT EXISTS pesquisa_preco (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    org_id      BIGINT NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
    username    VARCHAR(100) NOT NULL,
    valor_min   NUMERIC(10, 2) NOT NULL,
    valor_max   NUMERIC(10, 2) NOT NULL,
    comentario  TEXT,
    criado_em   TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_pesquisa_valor_min CHECK (valor_min >= 0),
    CONSTRAINT chk_pesquisa_valor_max CHECK (valor_max >= valor_min),
    CONSTRAINT uq_pesquisa_preco_usuario UNIQUE (usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_pesquisa_preco_org_id ON pesquisa_preco (org_id);
CREATE INDEX IF NOT EXISTS idx_pesquisa_preco_criado_em ON pesquisa_preco (criado_em DESC);

COMMENT ON TABLE pesquisa_preco IS 'Pesquisa de disposição a pagar (faixa mensal BRL) por usuário autenticado.';
