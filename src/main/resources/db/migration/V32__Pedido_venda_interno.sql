ALTER TABLE pedidos_venda ADD COLUMN IF NOT EXISTS tipo_pedido VARCHAR(20) NOT NULL DEFAULT 'VENDA';
ALTER TABLE pedidos_venda ADD COLUMN IF NOT EXISTS funcionario_id BIGINT REFERENCES usuarios(id);
ALTER TABLE pedidos_venda ALTER COLUMN consumidor_id DROP NOT NULL;
ALTER TABLE pedidos_venda ALTER COLUMN forma_pagamento DROP NOT NULL;
ALTER TABLE pedidos_venda ALTER COLUMN condicao_pagamento DROP NOT NULL;
