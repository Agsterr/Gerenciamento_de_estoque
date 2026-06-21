-- Senha em texto da última criação/reset, para repasse pelo admin (não substitui o hash de login).

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS senha_registrada VARCHAR(128);

COMMENT ON COLUMN usuarios.senha_registrada IS 'Última senha conhecida (criação/reset/admin). Para cópia pelo gestor.';
