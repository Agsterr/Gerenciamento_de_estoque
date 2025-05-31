package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentacaoProdutoRepository extends JpaRepository<MovimentacaoProduto, Long> {

    /**
     * Busca todas as movimentações de uma organização.
     */
    List<MovimentacaoProduto> findByOrgId(Long orgId);

    /**
     * Busca todas as movimentações por tipo (ENTRADA/SAIDA) e organização.
     */
    List<MovimentacaoProduto> findByTipoAndOrgId(TipoMovimentacao tipo, Long orgId);

    /**
     * Busca todas as movimentações de um produto específico dentro de uma organização.
     */
    List<MovimentacaoProduto> findByProdutoIdAndOrgId(Long produtoId, Long orgId);

    /**
     * Busca todas as movimentações de um produto e tipo específicos dentro de uma organização.
     */
    List<MovimentacaoProduto> findByProdutoIdAndTipoAndOrgId(Long produtoId, TipoMovimentacao tipo, Long orgId);

    /**
     * Busca todas as movimentações de uma organização entre duas datas (intervalo de LocalDateTime).
     */
    List<MovimentacaoProduto> findByDataHoraBetweenAndOrgId(LocalDateTime inicio, LocalDateTime fim, Long orgId);

    /**
     * Busca todas as movimentações de um tipo específico (ENTRADA/SAIDA) dentro de uma organização e intervalo de datas.
     */
    List<MovimentacaoProduto> findByTipoAndDataHoraBetweenAndOrgId(TipoMovimentacao tipo, LocalDateTime inicio, LocalDateTime fim, Long orgId);

    // -------------------------------------------------------------------------------------
    // MÉTODOS “Opção B” – usando intervalo de LocalDateTime para evitar funções específicas de dialeto
    // -------------------------------------------------------------------------------------

    /**
     * Soma as quantidades de movimentações de um tipo (ENTRADA/SAIDA) dentro de um intervalo de datas e organização.
     *
     * Exemplo de uso no serviço:
     *   LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0);
     *   LocalDateTime fim = inicio.plusMonths(1);
     *   Integer total = repository.somaPorTipoEIntervaloData(tipo, inicio, fim, orgId);
     */
    @Query("SELECT COALESCE(SUM(m.quantidade), 0) " +
            "FROM MovimentacaoProduto m " +
            "WHERE m.tipo = :tipo " +
            "  AND m.dataHora >= :inicio " +
            "  AND m.dataHora < :fim " +
            "  AND m.org.id = :orgId")
    Integer somaPorTipoEIntervaloData(
            @Param("tipo") TipoMovimentacao tipo,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("orgId") Long orgId
    );

    /**
     * Busca todas as movimentações de um tipo (ENTRADA/SAIDA) dentro de um intervalo de datas e organização.
     *
     * Exemplo de uso no serviço:
     *   LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0);
     *   LocalDateTime fim = inicio.plusMonths(1);
     *   List<MovimentacaoProduto> movs = repository.findMovimentacoesPorTipoEIntervalo(
     *         tipo, inicio, fim, orgId);
     */
    @Query("SELECT m " +
            "FROM MovimentacaoProduto m " +
            "WHERE m.tipo = :tipo " +
            "  AND m.dataHora >= :inicio " +
            "  AND m.dataHora < :fim " +
            "  AND m.org.id = :orgId")
    List<MovimentacaoProduto> findMovimentacoesPorTipoEIntervalo(
            @Param("tipo") TipoMovimentacao tipo,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("orgId") Long orgId
    );

    /**
     * Busca todas as movimentações de qualquer tipo dentro de um intervalo de datas e organização.
     *
     * Exemplo de uso no serviço:
     *   LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0);
     *   LocalDateTime fim = inicio.plusMonths(1);
     *   List<MovimentacaoProduto> movs = repository.findMovimentacoesPorIntervalo(inicio, fim, orgId);
     */
    @Query("SELECT m " +
            "FROM MovimentacaoProduto m " +
            "WHERE m.dataHora >= :inicio " +
            "  AND m.dataHora < :fim " +
            "  AND m.org.id = :orgId")
    List<MovimentacaoProduto> findMovimentacoesPorIntervalo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("orgId") Long orgId
    );

//  ***** Observação *****
//  Todos os métodos acima incluem sempre a condição `m.org.id = :orgId`, garantindo que as buscas
//  respeitem o multitenant (cada organização só vê suas movimentações).
}
