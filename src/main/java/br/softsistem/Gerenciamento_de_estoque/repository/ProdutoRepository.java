package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // Busca um produto pelo nome e pela organização
    Produto findByNomeAndOrgId(String nome, Long orgId);

    // Busca um produto pelo ID e pela organização, retornando Optional
    Optional<Produto> findByIdAndOrgId(Long id, Long orgId);

    // Busca todos os produtos ativos com paginação e organização
    Page<Produto> findByAtivoTrueAndOrgId(Long orgId, Pageable pageable);

    // Total de entradas de um produto por ano e organização
    @Query("SELECT COALESCE(SUM(p.quantidadeEntrada), 0) FROM Produto p WHERE FUNCTION('YEAR', p.dataEntrada) = :ano AND p.org.id = :orgId")
    BigDecimal totalEntradasPorAno(Long orgId, int ano);

    // Total de entradas de um produto por mês e organização
    @Query("SELECT COALESCE(SUM(p.quantidadeEntrada), 0) FROM Produto p WHERE FUNCTION('YEAR', p.dataEntrada) = :ano AND FUNCTION('MONTH', p.dataEntrada) = :mes AND p.org.id = :orgId")
    BigDecimal totalEntradasPorMes(Long orgId, int ano, int mes);

    // Total de entradas de um produto por semana e organização
    @Query("SELECT COALESCE(SUM(p.quantidadeEntrada), 0) FROM Produto p WHERE p.dataEntrada BETWEEN :inicioSemana AND :fimSemana AND p.org.id = :orgId")
    BigDecimal totalEntradasPorSemana(Long orgId, LocalDate inicioSemana, LocalDate fimSemana);

    // Total de entradas de um produto por dia e organização
    @Query("SELECT COALESCE(SUM(p.quantidadeEntrada), 0) FROM Produto p WHERE p.dataEntrada = :dia AND p.org.id = :orgId")
    BigDecimal totalEntradasPorDia(Long orgId, LocalDate dia);

    // Total de saídas de um produto por ano e organização
    @Query("SELECT COALESCE(SUM(p.quantidadeSaida), 0) FROM Produto p WHERE FUNCTION('YEAR', p.dataSaida) = :ano AND p.org.id = :orgId")
    BigDecimal totalSaidasPorAno(Long orgId, int ano);

    // Total de saídas de um produto por mês e organização
    @Query("SELECT COALESCE(SUM(p.quantidadeSaida), 0) FROM Produto p WHERE FUNCTION('YEAR', p.dataSaida) = :ano AND FUNCTION('MONTH', p.dataSaida) = :mes AND p.org.id = :orgId")
    BigDecimal totalSaidasPorMes(Long orgId, int ano, int mes);

    // Total de saídas de um produto por semana e organização
    @Query("SELECT COALESCE(SUM(p.quantidadeSaida), 0) FROM Produto p WHERE p.dataSaida BETWEEN :inicioSemana AND :fimSemana AND p.org.id = :orgId")
    BigDecimal totalSaidasPorSemana(Long orgId, LocalDate inicioSemana, LocalDate fimSemana);

    // Total de saídas de um produto por dia e organização
    @Query("SELECT COALESCE(SUM(p.quantidadeSaida), 0) FROM Produto p WHERE p.dataSaida = :dia AND p.org.id = :orgId")
    BigDecimal totalSaidasPorDia(Long orgId, LocalDate dia);
}
