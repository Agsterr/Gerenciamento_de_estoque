-- Migration para adicionar controle de ordem temporal de status em pagamentos
-- Versão: V18
-- Descrição: Adicionar campo last_status_update_at para prevenir regressão de status
--            quando webhooks chegam fora de ordem

-- Adicionar campo de timestamp da última atualização de status
ALTER TABLE payments
ADD COLUMN last_status_update_at TIMESTAMP;

-- Comentário explicativo
COMMENT ON COLUMN payments.last_status_update_at IS 
'Timestamp da última atualização de status processada. Usado para prevenir regressão de status quando webhooks chegam fora de ordem. Sempre baseado no date_last_updated da API do Mercado Pago (fonte da verdade).';

-- Índice para melhorar performance de consultas por timestamp
CREATE INDEX idx_payments_last_status_update_at ON payments(last_status_update_at);







