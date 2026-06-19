-- Migration para armazenar preapproval_plan_id do Mercado Pago nos planos
-- Versão: V21
-- Descrição: Adiciona coluna mercado_pago_preapproval_plan_id na tabela plans

ALTER TABLE plans
ADD COLUMN IF NOT EXISTS mercado_pago_preapproval_plan_id VARCHAR(255) UNIQUE;

CREATE INDEX IF NOT EXISTS idx_plans_mercado_pago_preapproval_plan_id
ON plans(mercado_pago_preapproval_plan_id);

COMMENT ON COLUMN plans.mercado_pago_preapproval_plan_id IS 'ID do PreapprovalPlan no Mercado Pago (plano associado)';

