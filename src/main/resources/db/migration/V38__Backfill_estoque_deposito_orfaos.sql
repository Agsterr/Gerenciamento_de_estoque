-- Aloca estoque global (produtos.quantidade) não distribuído em depósitos ao depósito padrão de cada org.
-- Corrige produtos criados antes do módulo estoque_deposito ou sem alocação no depósito padrão.

INSERT INTO depositos (nome, padrao, ativo, org_id)
SELECT 'Depósito Principal', true, true, o.id
FROM orgs o
WHERE NOT EXISTS (
    SELECT 1 FROM depositos d WHERE d.org_id = o.id AND d.padrao = true
);

WITH orphan AS (
    SELECT p.id AS produto_id,
           p.org_id,
           COALESCE(p.quantidade, 0) - COALESCE(SUM(ed.quantidade), 0) AS diff
    FROM produtos p
    LEFT JOIN estoque_deposito ed ON ed.produto_id = p.id
    WHERE COALESCE(p.ativo, true) = true
      AND COALESCE(p.quantidade, 0) > 0
    GROUP BY p.id, p.org_id, p.quantidade
    HAVING COALESCE(p.quantidade, 0) > COALESCE(SUM(ed.quantidade), 0)
),
default_dep AS (
    SELECT DISTINCT ON (d.org_id) d.id AS deposito_id, d.org_id
    FROM depositos d
    WHERE d.padrao = true AND d.ativo = true
    ORDER BY d.org_id, d.id
)
INSERT INTO estoque_deposito (produto_id, deposito_id, quantidade, org_id)
SELECT o.produto_id, dd.deposito_id, o.diff, o.org_id
FROM orphan o
JOIN default_dep dd ON dd.org_id = o.org_id
ON CONFLICT (produto_id, deposito_id)
DO UPDATE SET quantidade = estoque_deposito.quantidade + EXCLUDED.quantidade;
