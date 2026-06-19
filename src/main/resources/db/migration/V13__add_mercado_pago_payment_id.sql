-- Migration para adicionar suporte a pagamentos do Mercado Pago
-- Versão: V13
-- Descrição: Adiciona coluna mercado_pago_payment_id na tabela payments para rastrear pagamentos do Mercado Pago

ALTER TABLE payments
ADD COLUMN mercado_pago_payment_id BIGINT UNIQUE;

-- Índice para melhor performance nas buscas por ID do Mercado Pago
CREATE INDEX idx_payments_mercado_pago_payment_id ON payments(mercado_pago_payment_id);

-- Comentário na coluna
COMMENT ON COLUMN payments.mercado_pago_payment_id IS 'ID do pagamento no Mercado Pago (para idempotência e rastreamento)';







