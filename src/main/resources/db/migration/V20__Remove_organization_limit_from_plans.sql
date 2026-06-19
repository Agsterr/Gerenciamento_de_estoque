-- Migration para remover limite de organizações dos planos
-- Criado em: 2025-01-15
-- Descrição: Organizações não devem ser limitadas - cada organização é um cliente que contrata a aplicação

-- Atualizar todos os planos para ter max_organizations = NULL
-- Cada organização é um cliente que contrata a aplicação, então não faz sentido limitar
UPDATE plans 
SET max_organizations = NULL
WHERE max_organizations IS NOT NULL;

-- Comentário explicativo
COMMENT ON COLUMN plans.max_organizations IS 'DEPRECATED: Organizações não são limitadas - cada organização é um cliente que contrata a aplicação (sempre NULL)';







