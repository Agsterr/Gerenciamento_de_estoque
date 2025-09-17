-- Migration para criar tabela de pagamentos
-- Versão: V11
-- Descrição: Criar tabela payments para gerenciar pagamentos de assinaturas

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'REFUNDED')),
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    stripe_invoice_id VARCHAR(255),
    currency VARCHAR(3) DEFAULT 'BRL',
    payment_method VARCHAR(255),
    failure_reason TEXT,
    paid_at TIMESTAMP,
    failed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Chave estrangeira
    CONSTRAINT fk_payments_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
);

-- Índices para melhor performance
CREATE INDEX idx_payments_subscription_id ON payments(subscription_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_stripe_payment_intent_id ON payments(stripe_payment_intent_id);
CREATE INDEX idx_payments_stripe_invoice_id ON payments(stripe_invoice_id);
CREATE INDEX idx_payments_paid_at ON payments(paid_at);
CREATE INDEX idx_payments_failed_at ON payments(failed_at);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- Índice composto para pagamentos bem-sucedidos por assinatura
CREATE INDEX idx_payments_subscription_succeeded ON payments(subscription_id, paid_at) WHERE status = 'SUCCEEDED';

-- Índice composto para pagamentos falhados por assinatura
CREATE INDEX idx_payments_subscription_failed ON payments(subscription_id, failed_at) WHERE status = 'FAILED';

-- Comentários nas colunas
COMMENT ON TABLE payments IS 'Tabela de pagamentos de assinaturas';
COMMENT ON COLUMN payments.id IS 'Identificador único do pagamento';
COMMENT ON COLUMN payments.subscription_id IS 'ID da assinatura relacionada';
COMMENT ON COLUMN payments.amount IS 'Valor do pagamento';
COMMENT ON COLUMN payments.status IS 'Status do pagamento';
COMMENT ON COLUMN payments.stripe_payment_intent_id IS 'ID do payment intent no Stripe';
COMMENT ON COLUMN payments.stripe_invoice_id IS 'ID da invoice no Stripe';
COMMENT ON COLUMN payments.currency IS 'Moeda do pagamento (padrão BRL)';
COMMENT ON COLUMN payments.payment_method IS 'Método de pagamento utilizado';
COMMENT ON COLUMN payments.failure_reason IS 'Motivo da falha do pagamento';
COMMENT ON COLUMN payments.paid_at IS 'Data e hora do pagamento bem-sucedido';
COMMENT ON COLUMN payments.failed_at IS 'Data e hora da falha do pagamento';
COMMENT ON COLUMN payments.created_at IS 'Data de criação do registro';
COMMENT ON COLUMN payments.updated_at IS 'Data da última atualização do registro';