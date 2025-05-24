package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentacaoProdutoRepository extends JpaRepository<MovimentacaoProduto, Long> {

    List<MovimentacaoProduto> findByOrgId(Long orgId);

    List<MovimentacaoProduto> findByTipoAndOrgId(TipoMovimentacao tipo, Long orgId);

    List<MovimentacaoProduto> findByProdutoIdAndOrgId(Long produtoId, Long orgId);

    List<MovimentacaoProduto> findByProdutoIdAndTipoAndOrgId(Long produtoId, TipoMovimentacao tipo, Long orgId);

    List<MovimentacaoProduto> findByDataHoraBetweenAndOrgId(LocalDateTime inicio, LocalDateTime fim, Long orgId);

    List<MovimentacaoProduto> findByTipoAndDataHoraBetweenAndOrgId(TipoMovimentacao tipo, LocalDateTime inicio, LocalDateTime fim, Long orgId);

    // Buscar por ano
    @Query("SELECT m FROM MovimentacaoProduto m WHERE FUNCTION('YEAR', m.dataHora) = :ano AND m.org.id = :orgId")
    List<MovimentacaoProduto> findByAnoAndOrgId(int ano, Long orgId);

    // Buscar por mês e ano
    @Query("SELECT m FROM MovimentacaoProduto m WHERE FUNCTION('YEAR', m.dataHora) = :ano AND FUNCTION('MONTH', m.dataHora) = :mes AND m.org.id = :orgId")
    List<MovimentacaoProduto> findByAnoAndMesAndOrgId(int ano, int mes, Long orgId);

    // Total por tipo e ano
    @Query("SELECT COALESCE(SUM(m.quantidade), 0) FROM MovimentacaoProduto m WHERE m.tipo = :tipo AND FUNCTION('YEAR', m.dataHora) = :ano AND m.org.id = :orgId")
    Integer totalPorAno(TipoMovimentacao tipo, int ano, Long orgId);

    // Total por tipo, mês e ano
    @Query("SELECT COALESCE(SUM(m.quantidade), 0) FROM MovimentacaoProduto m WHERE m.tipo = :tipo AND FUNCTION('YEAR', m.dataHora) = :ano AND FUNCTION('MONTH', m.dataHora) = :mes AND m.org.id = :orgId")
    Integer totalPorMes(TipoMovimentacao tipo, int ano, int mes, Long orgId);
}
