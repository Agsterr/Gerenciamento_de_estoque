package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Produto findByNomeAndOrgId(String nome, Long orgId);

    Optional<Produto> findByIdAndOrgId(Long id, Long orgId);

    Page<Produto> findByAtivoTrueAndOrgId(Long orgId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.quantidadeEntrada), 0) FROM Produto p WHERE EXTRACT(YEAR FROM p.dataEntrada) = :ano AND p.org.id = :orgId")
    BigDecimal totalEntradasPorAno(@Param("orgId") Long orgId, @Param("ano") int ano);

    @Query("SELECT COALESCE(SUM(p.quantidadeEntrada), 0) FROM Produto p WHERE EXTRACT(YEAR FROM p.dataEntrada) = :ano AND EXTRACT(MONTH FROM p.dataEntrada) = :mes AND p.org.id = :orgId")
    BigDecimal totalEntradasPorMes(@Param("orgId") Long orgId, @Param("ano") int ano, @Param("mes") int mes);

    @Query("SELECT COALESCE(SUM(p.quantidadeEntrada), 0) FROM Produto p WHERE p.dataEntrada BETWEEN :inicioSemana AND :fimSemana AND p.org.id = :orgId")
    BigDecimal totalEntradasPorSemana(@Param("orgId") Long orgId, @Param("inicioSemana") LocalDate inicioSemana, @Param("fimSemana") LocalDate fimSemana);

    @Query("SELECT COALESCE(SUM(p.quantidadeEntrada), 0) FROM Produto p WHERE CAST(p.dataEntrada AS date) = :dia AND p.org.id = :orgId")
    BigDecimal totalEntradasPorDia(@Param("orgId") Long orgId, @Param("dia") LocalDate dia);

    @Query("SELECT COALESCE(SUM(p.quantidadeSaida), 0) FROM Produto p WHERE EXTRACT(YEAR FROM p.dataSaida) = :ano AND p.org.id = :orgId")
    BigDecimal totalSaidasPorAno(@Param("orgId") Long orgId, @Param("ano") int ano);

    @Query("SELECT COALESCE(SUM(p.quantidadeSaida), 0) FROM Produto p WHERE EXTRACT(YEAR FROM p.dataSaida) = :ano AND EXTRACT(MONTH FROM p.dataSaida) = :mes AND p.org.id = :orgId")
    BigDecimal totalSaidasPorMes(@Param("orgId") Long orgId, @Param("ano") int ano, @Param("mes") int mes);

    @Query("SELECT COALESCE(SUM(p.quantidadeSaida), 0) FROM Produto p WHERE p.dataSaida BETWEEN :inicioSemana AND :fimSemana AND p.org.id = :orgId")
    BigDecimal totalSaidasPorSemana(@Param("orgId") Long orgId, @Param("inicioSemana") LocalDate inicioSemana, @Param("fimSemana") LocalDate fimSemana);

    @Query("SELECT COALESCE(SUM(p.quantidadeSaida), 0) FROM Produto p WHERE CAST(p.dataSaida AS date) = :dia AND p.org.id = :orgId")
    BigDecimal totalSaidasPorDia(@Param("orgId") Long orgId, @Param("dia") LocalDate dia);
}
