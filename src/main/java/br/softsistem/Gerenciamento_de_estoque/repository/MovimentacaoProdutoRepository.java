package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentacaoProdutoRepository extends JpaRepository<MovimentacaoProduto, Long> {

    /**
     * Busca todas as movimentações de um produto pelo ID e pela organização, com suporte à paginação.
     * @param produtoId ID do produto
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações do produto na organização
     */
    Page<MovimentacaoProduto> findByProdutoIdAndOrgId(Long produtoId, Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de uma organização, com suporte à paginação.
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações da organização
     */
    Page<MovimentacaoProduto> findByOrgId(Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações por tipo (ENTRADA/SAIDA) e organização, com suporte à paginação.
     * @param tipo tipo de movimentação (ENTRADA ou SAIDA)
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações do tipo na organização
     */
    Page<MovimentacaoProduto> findByTipoAndOrgId(TipoMovimentacao tipo, Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de um produto e tipo específicos dentro de uma organização, com suporte à paginação.
     * @param produtoId ID do produto
     * @param tipo tipo de movimentação
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações filtradas
     */
    Page<MovimentacaoProduto> findByProdutoIdAndTipoAndOrgId(Long produtoId, TipoMovimentacao tipo, Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de uma organização entre duas datas, com suporte à paginação.
     * @param inicio data/hora inicial
     * @param fim data/hora final
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações no intervalo
     */
    Page<MovimentacaoProduto> findByDataHoraBetweenAndOrgId(LocalDateTime inicio, LocalDateTime fim, Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de um tipo específico dentro de uma organização e intervalo de datas, com suporte à paginação.
     * @param tipo tipo de movimentação
     * @param inicio data/hora inicial
     * @param fim data/hora final
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações filtradas
     */
    Page<MovimentacaoProduto> findByTipoAndDataHoraBetweenAndOrgId(TipoMovimentacao tipo, LocalDateTime inicio, LocalDateTime fim, Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de um tipo dentro de um intervalo de datas e organização, com suporte à paginação.
     * @param tipo tipo de movimentação
     * @param inicio data/hora inicial
     * @param fim data/hora final
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações filtradas
     */
    @Query("SELECT m FROM MovimentacaoProduto m WHERE m.tipo = :tipo AND m.dataHora >= :inicio AND m.dataHora < :fim AND m.org.id = :orgId")
    Page<MovimentacaoProduto> findMovimentacoesPorTipoEIntervalo(@Param("tipo") TipoMovimentacao tipo, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("orgId") Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de qualquer tipo dentro de um intervalo de datas e organização, com suporte à paginação.
     * @param inicio data/hora inicial
     * @param fim data/hora final
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações no intervalo
     */
    @Query("SELECT m FROM MovimentacaoProduto m WHERE m.dataHora >= :inicio AND m.dataHora < :fim AND m.org.id = :orgId")
    Page<MovimentacaoProduto> findMovimentacoesPorIntervalo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("orgId") Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de um produto pelo nome e pela organização, com suporte à paginação.
     * @param nome nome do produto (pode ser parcial)
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações do produto
     */
    @Query("SELECT m FROM MovimentacaoProduto m WHERE m.produto.nome LIKE %:nome% AND m.org.id = :orgId")
    Page<MovimentacaoProduto> findByProdutoNomeAndOrgId(@Param("nome") String nome, @Param("orgId") Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de um produto por categoria e pela organização, com suporte à paginação.
     * @param categoria nome da categoria (pode ser parcial)
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações da categoria
     */
    @Query("SELECT m FROM MovimentacaoProduto m WHERE m.produto.categoria.nome LIKE %:categoria% AND m.org.id = :orgId")
    Page<MovimentacaoProduto> findByProdutoCategoriaAndOrgId(@Param("categoria") String categoria, @Param("orgId") Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de uma organização, por produto, nome ou categoria, e por intervalo de datas, com suporte à paginação.
     * @param nome nome do produto (pode ser parcial)
     * @param categoria nome da categoria (pode ser parcial)
     * @param produtoId ID do produto
     * @param inicio data/hora inicial
     * @param fim data/hora final
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações filtradas
     */
    @Query("SELECT m FROM MovimentacaoProduto m WHERE (m.produto.nome LIKE %:nome% OR m.produto.categoria.nome LIKE %:categoria% OR m.produto.id = :produtoId) AND m.dataHora >= :inicio AND m.dataHora < :fim AND m.org.id = :orgId")
    Page<MovimentacaoProduto> findByProdutoNomeCategoriaIdAndIntervalo(@Param("nome") String nome, @Param("categoria") String categoria, @Param("produtoId") Long produtoId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("orgId") Long orgId, Pageable pageable);

    /**
     * Busca todas as movimentações de tipos (ENTRADA e SAIDA) para uma organização, com suporte à paginação.
     * @param tipos lista de tipos de movimentação (ENTRADA, SAIDA)
     * @param orgId ID da organização
     * @param pageable informações de paginação
     * @return página de movimentações dos tipos na organização
     */
    Page<MovimentacaoProduto> findByTipoInAndOrgId(List<TipoMovimentacao> tipos, Long orgId, Pageable pageable);

    // Vinculo com entrega para facilitar edição/remoção de movimentações relacionadas a entregas
    java.util.Optional<MovimentacaoProduto> findByEntregaId(Long entregaId);
    void deleteByEntregaId(Long entregaId);

    /**
     * Busca a movimentação mais recente de um produto específico.
     * @param produtoId ID do produto
     * @param orgId ID da organização
     * @return movimentação mais recente do produto
     */
    @Query("SELECT m FROM MovimentacaoProduto m WHERE m.produto.id = :produtoId AND m.org.id = :orgId ORDER BY m.dataHora DESC")
    java.util.Optional<MovimentacaoProduto> findFirstByProdutoIdAndOrgIdOrderByDataHoraDesc(@Param("produtoId") Long produtoId, @Param("orgId") Long orgId);
}
