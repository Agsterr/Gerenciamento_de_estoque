CREATE TABLE entrega (
    id BIGSERIAL PRIMARY KEY,
    consumidor_id BIGINT NOT NULL,
    entregador_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade INT NOT NULL,
    horario_entrega TIMESTAMP NOT NULL,
    CONSTRAINT fk_consumidor FOREIGN KEY (consumidor_id) REFERENCES consumidor(id),
    CONSTRAINT fk_entregador FOREIGN KEY (entregador_id) REFERENCES usuarios(id),
    CONSTRAINT fk_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
);
