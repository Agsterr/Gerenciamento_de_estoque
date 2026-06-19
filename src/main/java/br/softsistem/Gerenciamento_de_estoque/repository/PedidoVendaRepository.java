package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusPedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.model.PedidoVenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoVendaRepository extends JpaRepository<PedidoVenda, Long> {
    Page<PedidoVenda> findByOrgIdOrderByDataHoraDesc(Long orgId, Pageable pageable);
    Optional<PedidoVenda> findByIdAndOrgId(Long id, Long orgId);
    long countByOrgId(Long orgId);
    long countByOrgIdAndStatus(Long orgId, StatusPedidoVenda status);

    @Query("SELECT COUNT(p) FROM PedidoVenda p WHERE p.org.id = :orgId AND p.status = :status AND p.dataHora >= :inicio AND p.dataHora < :fim")
    long countConfirmadosNoPeriodo(@Param("orgId") Long orgId, @Param("status") StatusPedidoVenda status,
                                   @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    boolean existsByNumeroAndOrgId(String numero, Long orgId);

    @Query("SELECT COALESCE(SUM(p.valorTotal), 0) FROM PedidoVenda p WHERE p.org.id = :orgId AND p.status = :status AND p.tipoPedido = br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoPedidoVenda.VENDA AND p.dataHora >= :inicio AND p.dataHora < :fim")
    BigDecimal sumValorTotal(@Param("orgId") Long orgId, @Param("status") StatusPedidoVenda status,
                             @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query(value = """
            SELECT TO_CHAR(date_trunc('month', p.data_hora), 'YYYY-MM') AS mes,
                   COALESCE(SUM(p.valor_total), 0) AS total
            FROM pedidos_venda p
            WHERE p.org_id = :orgId AND p.status = 'CONFIRMADO' AND p.tipo_pedido = 'VENDA'
              AND p.data_hora >= :desde
            GROUP BY date_trunc('month', p.data_hora)
            ORDER BY mes DESC
            LIMIT 6
            """, nativeQuery = true)
    List<Object[]> faturamentoPorMes(@Param("orgId") Long orgId, @Param("desde") LocalDateTime desde);

    @Query(value = """
            SELECT pr.nome, SUM(i.quantidade) AS qtd, SUM(i.subtotal) AS valor
            FROM pedido_venda_itens i
            JOIN pedidos_venda p ON p.id = i.pedido_id
            JOIN produtos pr ON pr.id = i.produto_id
            WHERE p.org_id = :orgId AND p.status = 'CONFIRMADO' AND p.tipo_pedido = 'VENDA'
              AND p.data_hora >= :desde
            GROUP BY pr.id, pr.nome
            ORDER BY qtd DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> topProdutosVendidos(@Param("orgId") Long orgId, @Param("desde") LocalDateTime desde);

    Page<PedidoVenda> findByOrgIdAndStatusAndDataHoraBetweenOrderByDataHoraDesc(
            Long orgId, StatusPedidoVenda status, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);
}
