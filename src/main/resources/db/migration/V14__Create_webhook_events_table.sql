-- Migration para criar tabela de eventos de webhook para idempotência persistente
-- Versão: V14
-- Descrição: Criar tabela webhook_events para garantir que webhooks não sejam processados mais de uma vez, mesmo após restart

CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Índice único já criado pela constraint UNIQUE
    CONSTRAINT uk_webhook_events_event_id UNIQUE (event_id)
);

-- Índice para melhor performance nas buscas por event_id
CREATE INDEX idx_webhook_events_event_id ON webhook_events(event_id);

-- Índice para buscas por data de criação (útil para limpeza de eventos antigos)
CREATE INDEX idx_webhook_events_created_at ON webhook_events(created_at);

-- Comentários nas colunas
COMMENT ON TABLE webhook_events IS 'Tabela para controle de idempotência de webhooks do Mercado Pago';
COMMENT ON COLUMN webhook_events.id IS 'Identificador único do registro';
COMMENT ON COLUMN webhook_events.event_id IS 'ID único do evento do webhook (vem do campo "id" do payload)';
COMMENT ON COLUMN webhook_events.created_at IS 'Data e hora de criação do registro';







