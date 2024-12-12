-- Criação da tabela role
CREATE TABLE role (
    id BIGSERIAL PRIMARY KEY,  -- Usando BIGSERIAL para o ID de incremento automático
    nome VARCHAR(255) NOT NULL UNIQUE  -- Nome da role (por exemplo, 'ROLE_USER', 'ROLE_ADMIN')
);

-- Inserindo roles padrões (opcional)
INSERT INTO role (nome) VALUES
    ('ROLE_USER'),
    ('ROLE_ADMIN');



