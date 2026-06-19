-- Migration para adicionar suporte a histórico de chargebacks
-- Versão: V17
-- Descrição: Criar tabela para registrar histórico de chargebacks e reclamações

-- Adicionar campo de disputa na tabela payments
ALTER TABLE payments
ADD COLUMN in_dispute BOOLEAN DEFAULT FALSE;

-- Adicionar campo de bloqueio de acesso na tabela subscriptions
ALTER TABLE subscriptions
ADD COLUMN access_blocked BOOLEAN DEFAULT FALSE;

-- Criar tabela de histórico de chargebacks
CREATE TABLE chargeback_history (
    id BIGSERIAL PRIMARY KEY,
    chargeback_id VARCHAR(255) NOT NULL,
    payment_id BIGINT NOT NULL,
    subscription_id BIGINT,
    user_id BIGINT,
    status VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    reason TEXT,
    amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Chaves estrangeiras
    CONSTRAINT fk_chargeback_history_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_chargeback_history_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE SET NULL
);

-- Índices para melhor performance
CREATE INDEX idx_chargeback_history_chargeback_id ON chargeback_history(chargeback_id);
CREATE INDEX idx_chargeback_history_payment_id ON chargeback_history(payment_id);
CREATE INDEX idx_chargeback_history_subscription_id ON chargeback_history(subscription_id);
CREATE INDEX idx_chargeback_history_user_id ON chargeback_history(user_id);
CREATE INDEX idx_chargeback_history_status ON chargeback_history(status);
CREATE INDEX idx_chargeback_history_created_at ON chargeback_history(created_at);

-- Índice único para evitar duplicatas
CREATE UNIQUE INDEX idx_chargeback_history_unique ON chargeback_history(chargeback_id, payment_id, status);

-- Comentários
COMMENT ON TABLE chargeback_history IS 'Histórico de chargebacks e reclamações';
COMMENT ON COLUMN chargeback_history.id IS 'Identificador único do registro';
COMMENT ON COLUMN chargeback_history.chargeback_id IS 'ID do chargeback no Mercado Pago';
COMMENT ON COLUMN chargeback_history.payment_id IS 'ID do pagamento relacionado';
COMMENT ON COLUMN chargeback_history.subscription_id IS 'ID da assinatura relacionada';
COMMENT ON COLUMN chargeback_history.user_id IS 'ID do usuário relacionado';
COMMENT ON COLUMN chargeback_history.status IS 'Status do chargeback (created, updated, resolved, etc.)';
COMMENT ON COLUMN chargeback_history.action IS 'Ação do evento (chargeback.created, chargeback.updated, etc.)';
COMMENT ON COLUMN chargeback_history.reason IS 'Motivo do chargeback';
COMMENT ON COLUMN chargeback_history.amount IS 'Valor do chargeback';
COMMENT ON COLUMN payments.in_dispute IS 'Indica se o pagamento está em disputa';
COMMENT ON COLUMN subscriptions.access_blocked IS 'Indica se o acesso ao serviço está bloqueado devido a chargeback';







