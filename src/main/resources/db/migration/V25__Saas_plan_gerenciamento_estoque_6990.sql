-- Plano único SaaS: Gerenciamento de Estoque — R$ 69,90/mês
-- Planos antigos (Profissional/Empresarial) ficam inativos para novas assinaturas.

UPDATE plans
SET is_active = false,
    updated_at = CURRENT_TIMESTAMP
WHERE type IN ('PROFESSIONAL', 'ENTERPRISE');

UPDATE plans
SET name = 'Gerenciamento de Estoque',
    description = 'Assinatura mensal do sistema de gerenciamento de estoque — acesso completo após o período de teste.',
    price = 69.90,
    max_users = 10,
    max_products = 2000,
    max_organizations = 3,
    has_reports = true,
    has_advanced_analytics = false,
    has_api_access = false,
    is_active = true,
    updated_at = CURRENT_TIMESTAMP
WHERE type = 'BASIC';

INSERT INTO plans (
    name, description, price, type,
    max_users, max_products, max_organizations,
    has_reports, has_advanced_analytics, has_api_access, is_active
)
SELECT
    'Gerenciamento de Estoque',
    'Assinatura mensal do sistema de gerenciamento de estoque — acesso completo após o período de teste.',
    69.90,
    'BASIC',
    10,
    2000,
    3,
    true,
    false,
    false,
    true
WHERE NOT EXISTS (SELECT 1 FROM plans WHERE type = 'BASIC');

COMMENT ON TABLE plans IS 'Planos locais do SaaS; no checkout Asaas o valor é enviado como cobrança avulsa mensal';
