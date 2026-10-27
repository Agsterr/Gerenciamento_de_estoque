-- Plano único SaaS: Gerenciamento de Estoque — R$ 99,90/mês (trial gratuito de 30 dias na aplicação)

UPDATE plans
SET price = 99.90,
    updated_at = CURRENT_TIMESTAMP
WHERE type = 'BASIC';
