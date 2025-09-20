-- Migration para inserir planos do Stripe baseados nas melhores práticas de SaaS
-- Criado em: $(date)
-- Descrição: Insere os três planos principais (Basic, Professional, Enterprise) com recursos bem definidos

-- Inserir planos do Stripe
INSERT INTO plans (
    name, 
    description, 
    price, 
    type, 
    max_users, 
    max_products, 
    max_organizations, 
    has_reports, 
    has_advanced_analytics, 
    has_api_access, 
    is_active, 
    created_at, 
    updated_at
) VALUES 
(
    'Básico',
    'Plano ideal para pequenas empresas que estão começando. Inclui funcionalidades essenciais para gerenciamento de estoque com relatórios básicos e suporte por email.',
    29.90,
    'BASIC',
    5,
    1000,
    1,
    true,
    false,
    false,
    true,
    NOW(),
    NOW()
),
(
    'Profissional',
    'Plano completo para empresas em crescimento. Inclui analytics avançado, relatórios detalhados e suporte prioritário para otimizar suas operações.',
    79.90,
    'PROFESSIONAL',
    25,
    10000,
    5,
    true,
    true,
    false,
    true,
    NOW(),
    NOW()
),
(
    'Empresarial',
    'Solução enterprise com recursos ilimitados. Inclui acesso completo à API, analytics avançado, relatórios personalizados e suporte dedicado 24/7.',
    199.90,
    'ENTERPRISE',
    NULL, -- Ilimitado
    NULL, -- Ilimitado
    NULL, -- Ilimitado
    true,
    true,
    true,
    true,
    NOW(),
    NOW()
);

-- Comentários sobre os recursos de cada plano
COMMENT ON TABLE plans IS 'Tabela de planos de assinatura integrados com Stripe';
COMMENT ON COLUMN plans.max_users IS 'Número máximo de usuários permitidos (NULL = ilimitado)';
COMMENT ON COLUMN plans.max_products IS 'Número máximo de produtos permitidos (NULL = ilimitado)';
COMMENT ON COLUMN plans.max_organizations IS 'Número máximo de organizações permitidas (NULL = ilimitado)';
COMMENT ON COLUMN plans.has_reports IS 'Se o plano inclui relatórios básicos';
COMMENT ON COLUMN plans.has_advanced_analytics IS 'Se o plano inclui analytics avançado';
COMMENT ON COLUMN plans.has_api_access IS 'Se o plano inclui acesso completo à API';
COMMENT ON COLUMN plans.stripe_price_id IS 'ID do preço no Stripe (será preenchido automaticamente)';
COMMENT ON COLUMN plans.stripe_product_id IS 'ID do produto no Stripe (será preenchido automaticamente)';