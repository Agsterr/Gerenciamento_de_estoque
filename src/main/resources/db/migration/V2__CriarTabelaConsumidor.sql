CREATE TABLE consumidor (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(11) UNIQUE,
    endereco VARCHAR(255)
);
