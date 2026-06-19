-- Adiciona coluna bypass_subscription e inicializa SUPER_ADMIN único com bypass ativo

-- 1) Adicionar coluna na tabela usuarios
ALTER TABLE usuarios
ADD COLUMN IF NOT EXISTS bypass_subscription BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN usuarios.bypass_subscription IS 'Quando TRUE, usuário pode acessar sem assinatura (apenas SUPER_ADMIN). Controlado via migration/script.';

-- 2) Garante unicidade de bypass ativo (apenas 1 usuário)
CREATE UNIQUE INDEX IF NOT EXISTS uniq_super_admin_bypass_true
ON usuarios (bypass_subscription)
WHERE bypass_subscription = TRUE;

-- 3) Inicializa bypass em um (e apenas um) SUPER_ADMIN existente
UPDATE usuarios
SET bypass_subscription = TRUE
WHERE id = (
    SELECT MIN(ur.usuario_id)
    FROM usuario_roles ur
    JOIN role r ON r.id = ur.role_id
    WHERE r.nome = 'ROLE_SUPER_ADMIN'
);

-- 4) Unicidade de role por organização (somente se a coluna org_id existir na tabela role)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'role'
          AND column_name = 'org_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_role_org_nome'
    ) THEN
        ALTER TABLE role ADD CONSTRAINT uq_role_org_nome UNIQUE (org_id, nome);
    END IF;
END $$;

