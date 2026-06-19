-- Migration para adicionar suporte a cartões do Mercado Pago
-- Versão: V16
-- Descrição: Adiciona campos para armazenar card_id e histórico de cartões

-- Adicionar card_id na tabela subscriptions
ALTER TABLE subscriptions
ADD COLUMN mercado_pago_card_id VARCHAR(255);

-- Criar índice para card_id
CREATE INDEX idx_subscriptions_mercado_pago_card_id ON subscriptions(mercado_pago_card_id);

-- Criar tabela de histórico de cartões
CREATE TABLE card_history (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    old_card_id VARCHAR(255),
    new_card_id VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) DEFAULT 'webhook',
    reason TEXT,
    
    -- Chave estrangeira
    CONSTRAINT fk_card_history_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
);

-- Índices para melhor performance
CREATE INDEX idx_card_history_subscription_id ON card_history(subscription_id);
CREATE INDEX idx_card_history_updated_at ON card_history(updated_at);
CREATE INDEX idx_card_history_new_card_id ON card_history(new_card_id);

-- Comentários
COMMENT ON TABLE card_history IS 'Histórico de alterações de cartões de pagamento';
COMMENT ON COLUMN card_history.id IS 'Identificador único do registro de histórico';
COMMENT ON COLUMN card_history.subscription_id IS 'ID da assinatura relacionada';
COMMENT ON COLUMN card_history.old_card_id IS 'ID do cartão anterior';
COMMENT ON COLUMN card_history.new_card_id IS 'ID do novo cartão';
COMMENT ON COLUMN card_history.updated_at IS 'Data/hora da atualização';
COMMENT ON COLUMN card_history.updated_by IS 'Origem da atualização (webhook, admin, etc.)';
COMMENT ON COLUMN card_history.reason IS 'Motivo da alteração do cartão';







