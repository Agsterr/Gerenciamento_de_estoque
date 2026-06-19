-- Migration para adicionar flags de processamento em webhook_events
-- Versão: V19
-- Descrição: Adicionar campos para rastrear se evento foi processado e permitir retry automático

-- Adicionar campo processed (já processado ou não)
ALTER TABLE webhook_events
ADD COLUMN processed BOOLEAN NOT NULL DEFAULT FALSE;

-- Adicionar campo processed_at (quando foi processado)
ALTER TABLE webhook_events
ADD COLUMN processed_at TIMESTAMP;

-- Adicionar campo error_message (mensagem de erro se falhou)
ALTER TABLE webhook_events
ADD COLUMN error_message TEXT;

-- Comentários explicativos
COMMENT ON COLUMN webhook_events.processed IS 
'Indica se o evento foi completamente processado. false = recebido mas não processado (pode ter falhado), true = processado com sucesso';

COMMENT ON COLUMN webhook_events.processed_at IS 
'Timestamp de quando o evento foi processado com sucesso. null = ainda não foi processado';

COMMENT ON COLUMN webhook_events.error_message IS 
'Mensagem de erro caso o processamento tenha falhado. null = sem erro ou ainda não processado';

-- Índices para melhorar performance de consultas
CREATE INDEX idx_webhook_events_processed ON webhook_events(processed);
CREATE INDEX idx_webhook_events_processed_created ON webhook_events(processed, created_at);







