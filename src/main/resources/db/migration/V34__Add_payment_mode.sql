ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(20);

COMMENT ON COLUMN subscriptions.payment_mode IS 'Modo Asaas: RECURRING, PIX ou BOLETO (cobrança avulsa mensal)';
