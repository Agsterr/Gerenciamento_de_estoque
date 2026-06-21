package br.softsistem.Gerenciamento_de_estoque.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remove dados operacionais de uma org demo, preservando usuários, roles e a própria org.
 */
@Service
public class DemoOrgPurgeService {

    private static final Logger log = LoggerFactory.getLogger(DemoOrgPurgeService.class);

    private final JdbcTemplate jdbcTemplate;

    public DemoOrgPurgeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void purgeOperationalData(Long orgId) {
        if (orgId == null) {
            return;
        }
        log.info("Limpando dados operacionais da org demo id={}", orgId);

        jdbcTemplate.update(
                "DELETE FROM pedido_venda_itens WHERE pedido_id IN (SELECT id FROM pedidos_venda WHERE org_id = ?)",
                orgId);
        jdbcTemplate.update("DELETE FROM pedidos_venda WHERE org_id = ?", orgId);
        jdbcTemplate.update(
                "DELETE FROM pedido_compra_itens WHERE pedido_id IN (SELECT id FROM pedidos_compra WHERE org_id = ?)",
                orgId);
        jdbcTemplate.update("DELETE FROM pedidos_compra WHERE org_id = ?", orgId);
        jdbcTemplate.update(
                "DELETE FROM contagem_inventario_itens WHERE contagem_id IN (SELECT id FROM contagens_inventario WHERE org_id = ?)",
                orgId);
        jdbcTemplate.update("DELETE FROM contagens_inventario WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM estoque_deposito WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM movimentacoes_produto WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM produtos WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM categorias WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM consumidor WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM fornecedores WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM depositos WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM auditoria_log WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM sugestoes WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM pesquisa_preco WHERE org_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM dispositivos_usuario WHERE org_id = ?", orgId);
    }
}
