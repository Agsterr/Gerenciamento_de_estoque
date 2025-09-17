-- Migration para criar tabela de assinaturas
-- Versão: V10
-- Descrição: Criar tabela subscriptions para gerenciar assinaturas de usuários

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('TRIAL', 'ACTIVE', 'CANCELED', 'PAST_DUE', 'EXPIRED', 'INCOMPLETE', 'INCOMPLETE_EXPIRED')),
    stripe_subscription_id VARCHAR(255) UNIQUE,
    stripe_customer_id VARCHAR(255),
    trial_start TIMESTAMP,
    trial_end TIMESTAMP,
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    canceled_at TIMESTAMP,
    ended_at TIMESTAMP,
    trial_warning_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Chaves estrangeiras
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE RESTRICT
);

-- Índices para melhor performance
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_stripe_subscription_id ON subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscriptions_stripe_customer_id ON subscriptions(stripe_customer_id);
CREATE INDEX idx_subscriptions_trial_end ON subscriptions(trial_end);
CREATE INDEX idx_subscriptions_current_period_end ON subscriptions(current_period_end);
CREATE INDEX idx_subscriptions_created_at ON subscriptions(created_at);

-- Índice composto para buscar assinatura ativa do usuário
CREATE INDEX idx_subscriptions_user_active ON subscriptions(user_id, status) WHERE status IN ('TRIAL', 'ACTIVE');

-- Índice para trials próximos do fim
CREATE INDEX idx_subscriptions_trial_ending ON subscriptions(trial_end, trial_warning_sent) WHERE status = 'TRIAL';

-- Comentários nas colunas
COMMENT ON TABLE subscriptions IS 'Tabela de assinaturas de usuários';
COMMENT ON COLUMN subscriptions.id IS 'Identificador único da assinatura';
COMMENT ON COLUMN subscriptions.user_id IS 'ID do usuário proprietário da assinatura';
COMMENT ON COLUMN subscriptions.plan_id IS 'ID do plano da assinatura';
COMMENT ON COLUMN subscriptions.status IS 'Status atual da assinatura';
COMMENT ON COLUMN subscriptions.stripe_subscription_id IS 'ID da assinatura no Stripe';
COMMENT ON COLUMN subscriptions.stripe_customer_id IS 'ID do cliente no Stripe';
COMMENT ON COLUMN subscriptions.trial_start IS 'Data de início do período de teste';
COMMENT ON COLUMN subscriptions.trial_end IS 'Data de fim do período de teste';
COMMENT ON COLUMN subscriptions.current_period_start IS 'Início do período atual de cobrança';
COMMENT ON COLUMN subscriptions.current_period_end IS 'Fim do período atual de cobrança';
COMMENT ON COLUMN subscriptions.canceled_at IS 'Data de cancelamento da assinatura';
COMMENT ON COLUMN subscriptions.ended_at IS 'Data de encerramento da assinatura';
COMMENT ON COLUMN subscriptions.trial_warning_sent IS 'Se o alerta de fim de trial foi enviado';
COMMENT ON COLUMN subscriptions.created_at IS 'Data de criação do registro';
COMMENT ON COLUMN subscriptions.updated_at IS 'Data da última atualização do registro';