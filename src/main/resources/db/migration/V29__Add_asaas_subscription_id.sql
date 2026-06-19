-- Assinatura recorrente no Asaas (sub_...)
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS asaas_subscription_id VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS idx_subscriptions_asaas_subscription_id
    ON subscriptions(asaas_subscription_id) WHERE asaas_subscription_id IS NOT NULL;

COMMENT ON COLUMN subscriptions.asaas_subscription_id IS 'ID da assinatura recorrente no Asaas (sub_...)';
