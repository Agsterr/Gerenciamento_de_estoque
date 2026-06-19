-- Colunas Asaas para assinaturas e pagamentos
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS asaas_customer_id VARCHAR(255);
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS asaas_payment_id VARCHAR(255);
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(50) DEFAULT 'ASAAS';

ALTER TABLE payments ADD COLUMN IF NOT EXISTS asaas_payment_id VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_asaas_payment_id ON payments(asaas_payment_id) WHERE asaas_payment_id IS NOT NULL;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS asaas_customer_id VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS cpf_cnpj VARCHAR(18);

COMMENT ON COLUMN subscriptions.asaas_customer_id IS 'ID do cliente no Asaas (cus_...)';
COMMENT ON COLUMN subscriptions.asaas_payment_id IS 'ID da cobrança pendente no Asaas (pay_...)';
COMMENT ON COLUMN subscriptions.payment_provider IS 'Provedor de pagamento: ASAAS ou MERCADOPAGO';
COMMENT ON COLUMN payments.asaas_payment_id IS 'ID da cobrança no Asaas';
COMMENT ON COLUMN usuarios.asaas_customer_id IS 'ID do cliente no Asaas';
COMMENT ON COLUMN usuarios.cpf_cnpj IS 'CPF/CNPJ do usuário para cobranças Asaas';
