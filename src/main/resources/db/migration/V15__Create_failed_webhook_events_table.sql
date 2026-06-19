-- Migration para criar tabela de eventos de webhook que falharam no processamento
-- Versão: V15
-- Descrição: Criar tabela failed_webhook_events para não perder eventos que falharem

CREATE TABLE failed_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    error_message TEXT,
    error_stack_trace TEXT,
    retry_count INTEGER DEFAULT 0,
    last_retry_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Índices para melhor performance
    CONSTRAINT idx_failed_webhook_events_event_id UNIQUE (event_id)
);

-- Índices para melhor performance nas buscas
CREATE INDEX idx_failed_webhook_events_event_type ON failed_webhook_events(event_type);
CREATE INDEX idx_failed_webhook_events_retry_count ON failed_webhook_events(retry_count);
CREATE INDEX idx_failed_webhook_events_created_at ON failed_webhook_events(created_at);

-- Comentários nas colunas
COMMENT ON TABLE failed_webhook_events IS 'Tabela para armazenar eventos de webhook que falharam no processamento';
COMMENT ON COLUMN failed_webhook_events.id IS 'Identificador único do registro';
COMMENT ON COLUMN failed_webhook_events.event_id IS 'ID único do evento do webhook';
COMMENT ON COLUMN failed_webhook_events.event_type IS 'Tipo do evento (payment, merchant_order, etc.)';
COMMENT ON COLUMN failed_webhook_events.payload IS 'Payload completo do webhook (JSON)';
COMMENT ON COLUMN failed_webhook_events.error_message IS 'Mensagem de erro que causou a falha';
COMMENT ON COLUMN failed_webhook_events.error_stack_trace IS 'Stack trace completo do erro';
COMMENT ON COLUMN failed_webhook_events.retry_count IS 'Número de tentativas de reprocessamento';
COMMENT ON COLUMN failed_webhook_events.last_retry_at IS 'Data/hora da última tentativa de reprocessamento';







