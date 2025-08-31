-- Criação da tabela orgs (organizações)
-- Esta tabela armazena as organizações do sistema

CREATE TABLE orgs (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Índice para melhorar performance nas consultas por nome
CREATE INDEX idx_orgs_nome ON orgs(nome);
CREATE INDEX idx_orgs_ativo ON orgs(ativo);