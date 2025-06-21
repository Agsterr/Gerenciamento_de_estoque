package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    // ======================================================
    // PAGINAÇÃO: listar todas as entregas de uma organização
    // ======================================================
    @Query("SELECT e " +
            "FROM Entrega e " +
            "WHERE e.org.id = :orgId")
    Page<Entrega> findByOrgId(@Param("orgId") Long orgId, Pageable pageable);

    // ======================================================
    // SOMA DE VALORES (deixamos, mas não vamos mais usar para retorno)
    // ======================================================
    @Query("SELECT COALESCE(SUM(e.valor), 0) " +
            "FROM Entrega e " +
            "WHERE e.horarioEntrega >= :inicio " +
            "  AND e.horarioEntrega <= :fim " +
            "  AND e.org.id = :orgId")
    java.math.BigDecimal totalPorIntervalo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim")    LocalDateTime fim,
            @Param("orgId")  Long orgId
    );

    @Query("SELECT COALESCE(SUM(e.valor), 0) " +
            "FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId " +
            "  AND e.horarioEntrega >= :inicio " +
            "  AND e.horarioEntrega <= :fim " +
            "  AND e.org.id = :orgId")
    java.math.BigDecimal totalPorIntervaloPorConsumidor(
            @Param("consumidorId") Long consumidorId,
            @Param("inicio")       LocalDateTime inicio,
            @Param("fim")          LocalDateTime fim,
            @Param("orgId")        Long orgId
    );

    // ======================================================
    // DETALHAMENTO: listar entregas em intervalo genérico com paginação
    // ======================================================
    @Query("SELECT e " +
            "FROM Entrega e " +
            "WHERE e.horarioEntrega >= :inicio " +
            "  AND e.horarioEntrega <= :fim " +
            "  AND e.org.id = :orgId " +
            "ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByHorarioEntregaBetweenAndOrgId(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim")    LocalDateTime fim,
            @Param("orgId")  Long orgId,
            Pageable pageable
    );

    // ======================================================
    // NOVOS MÉTODOS PARA BUSCAR POR CONSUMIDOR (DETALHADO) COM PAGINAÇÃO
    // ======================================================

    /**
     * Retorna todas as entregas de um consumidor em toda organização (sem intervalo de data) com paginação.
     */
    @Query("SELECT e " +
            "FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId " +
            "  AND e.org.id = :orgId " +
            "ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByConsumidorIdAndOrgId(
            @Param("consumidorId") Long consumidorId,
            @Param("orgId")        Long orgId,
            Pageable pageable
    );

    /**
     * Retorna todas as entregas de um consumidor em um intervalo de data/hora, filtrando pela organização com paginação.
     */
    @Query("SELECT e " +
            "FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId " +
            "  AND e.horarioEntrega >= :inicio " +
            "  AND e.horarioEntrega <= :fim " +
            "  AND e.org.id = :orgId " +
            "ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByConsumidorIdAndHorarioEntregaBetweenAndOrgId(
            @Param("consumidorId") Long consumidorId,
            @Param("inicio")       LocalDateTime inicio,
            @Param("fim")          LocalDateTime fim,
            @Param("orgId")        Long orgId,
            Pageable pageable
    );

    // ======================================================
    // NOVO MÉTODO PARA BUSCAR POR PRODUTO E ORGANIZAÇÃO COM PAGINAÇÃO
    // ======================================================
    /**
     * Retorna todas as entregas de uma organização que contenham um produto específico com paginação.
     */
    @Query("SELECT e " +
            "FROM Entrega e " +
            "WHERE e.produto.id = :produtoId " + // Agora estamos filtrando pelo produto diretamente
            "  AND e.org.id = :orgId " + // Garantindo que a entrega pertença à organização correta
            "ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByProdutoIdAndOrgId(
            @Param("produtoId") Long produtoId,
            @Param("orgId")     Long orgId,
            Pageable pageable
    );
}
