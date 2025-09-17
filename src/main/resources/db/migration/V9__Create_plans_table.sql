-- Migration para criar tabela de planos de assinatura
-- Versão: V9
-- Descrição: Criar tabela plans para gerenciar planos de assinatura

CREATE TABLE plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    type VARCHAR(50) NOT NULL CHECK (type IN ('BASIC', 'PROFESSIONAL', 'ENTERPRISE')),
    stripe_price_id VARCHAR(255) UNIQUE,
    stripe_product_id VARCHAR(255) UNIQUE,
    max_users INTEGER,
    max_products INTEGER,
    max_organizations INTEGER,
    has_reports BOOLEAN DEFAULT FALSE,
    has_advanced_analytics BOOLEAN DEFAULT FALSE,
    has_api_access BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para melhor performance
CREATE INDEX idx_plans_type ON plans(type);
CREATE INDEX idx_plans_is_active ON plans(is_active);
CREATE INDEX idx_plans_price ON plans(price);
CREATE INDEX idx_plans_stripe_price_id ON plans(stripe_price_id);
CREATE INDEX idx_plans_stripe_product_id ON plans(stripe_product_id);

-- Inserir planos padrão
INSERT INTO plans (name, description, price, type, max_users, max_products, max_organizations, has_reports, has_advanced_analytics, has_api_access) VALUES
('Básico', 'Plano básico com funcionalidades essenciais para pequenos negócios', 29.90, 'BASIC', 3, 100, 1, FALSE, FALSE, FALSE),
('Profissional', 'Plano profissional com recursos avançados para empresas em crescimento', 79.90, 'PROFESSIONAL', 10, 500, 3, TRUE, FALSE, FALSE),
('Empresarial', 'Plano empresarial com todos os recursos para grandes organizações', 149.90, 'ENTERPRISE', NULL, NULL, NULL, TRUE, TRUE, TRUE);

-- Comentários nas colunas
COMMENT ON TABLE plans IS 'Tabela de planos de assinatura';
COMMENT ON COLUMN plans.id IS 'Identificador único do plano';
COMMENT ON COLUMN plans.name IS 'Nome do plano';
COMMENT ON COLUMN plans.description IS 'Descrição detalhada do plano';
COMMENT ON COLUMN plans.price IS 'Preço mensal do plano em reais';
COMMENT ON COLUMN plans.type IS 'Tipo do plano (BASIC, PROFESSIONAL, ENTERPRISE)';
COMMENT ON COLUMN plans.stripe_price_id IS 'ID do preço no Stripe';
COMMENT ON COLUMN plans.stripe_product_id IS 'ID do produto no Stripe';
COMMENT ON COLUMN plans.max_users IS 'Número máximo de usuários (NULL = ilimitado)';
COMMENT ON COLUMN plans.max_products IS 'Número máximo de produtos (NULL = ilimitado)';
COMMENT ON COLUMN plans.max_organizations IS 'Número máximo de organizações (NULL = ilimitado)';
COMMENT ON COLUMN plans.has_reports IS 'Se o plano inclui relatórios';
COMMENT ON COLUMN plans.has_advanced_analytics IS 'Se o plano inclui analytics avançados';
COMMENT ON COLUMN plans.has_api_access IS 'Se o plano inclui acesso à API';
COMMENT ON COLUMN plans.is_active IS 'Se o plano está ativo para novas assinaturas';
COMMENT ON COLUMN plans.created_at IS 'Data de criação do registro';
COMMENT ON COLUMN plans.updated_at IS 'Data da última atualização do registro';