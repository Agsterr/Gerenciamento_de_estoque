     CREATE TABLE usuario_roles (
     usuario_id BIGINT NOT NULL, -- Relaciona com a tabela usuarios
     role_id BIGINT NOT NULL, -- Relaciona com a tabela role
     PRIMARY KEY (usuario_id, role_id),
     FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
     FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
 );




