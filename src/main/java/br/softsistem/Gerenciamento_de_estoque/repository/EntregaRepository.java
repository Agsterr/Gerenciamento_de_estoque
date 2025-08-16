package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repositório para operações de CRUD e consultas customizadas sobre Entrega.
 */
@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    // ======================================================
    // PAGINAÇÃO: listar todas as entregas de uma organização
    // ======================================================

    /**
     * Busca todas as entregas pertencentes à organização especificada, com paginação.
     *
     * @param orgId   ID da organização
     * @param pageable parâmetros de paginação (página atual, tamanho, ordenação)
     * @return página de entidades Entrega filtradas pela organização
     */
    @Query("SELECT e FROM Entrega e WHERE e.org.id = :orgId")
    Page<Entrega> findByOrgId(@Param("orgId") Long orgId, Pageable pageable);

    // ======================================================
    // SOMA DE VALORES (usados em relatórios de intervalo)
    // ======================================================

    /**
     * Calcula a soma total dos valores de todas as entregas realizadas no intervalo de datas especificado.
     *
     * @param inicio data/hora inicial do intervalo (inclusive)
     * @param fim    data/hora final do intervalo (inclusive)
     * @param orgId  ID da organização
     * @return soma dos valores das entregas no intervalo; retorna 0 se não houver entregas
     */
    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM Entrega e " +
            "WHERE e.horarioEntrega >= :inicio AND e.horarioEntrega <= :fim AND e.org.id = :orgId")
    java.math.BigDecimal totalPorIntervalo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim")    LocalDateTime fim,
            @Param("orgId")  Long orgId
    );

    /**
     * Calcula a soma total dos valores de todas as entregas de um consumidor no intervalo de datas especificado.
     *
     * @param consumidorId ID do consumidor
     * @param inicio       data/hora inicial do intervalo (inclusive)
     * @param fim          data/hora final do intervalo (inclusive)
     * @param orgId        ID da organização
     * @return soma dos valores das entregas do consumidor no intervalo; retorna 0 se não houver entregas
     */
    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId " +
            "AND e.horarioEntrega >= :inicio AND e.horarioEntrega <= :fim " +
            "AND e.org.id = :orgId")
    java.math.BigDecimal totalPorIntervaloPorConsumidor(
            @Param("consumidorId") Long consumidorId,
            @Param("inicio")       LocalDateTime inicio,
            @Param("fim")          LocalDateTime fim,
            @Param("orgId")        Long orgId
    );

    /**
     * Calcula a soma total dos valores de todas as entregas de um consumidor em toda a organização.
     *
     * @param consumidorId ID do consumidor
     * @param orgId        ID da organização
     * @return soma dos valores de todas as entregas do consumidor; retorna 0 se não houver entregas
     */
    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId AND e.org.id = :orgId")
    java.math.BigDecimal totalPorConsumidor(
            @Param("consumidorId") Long consumidorId,
            @Param("orgId")        Long orgId
    );

    // ======================================================
    // DETALHAMENTO: consultas com paginação e filtro de datas
    // ======================================================

    /**
     * Lista entregas de uma organização em um intervalo de datas, com paginação.
     *
     * @param inicio   data/hora inicial do intervalo (inclusive)
     * @param fim      data/hora final do intervalo (inclusive)
     * @param orgId    ID da organização
     * @param pageable parâmetros de paginação
     * @return página de entregas dentro do intervalo
     */
    @Query("SELECT e FROM Entrega e " +
            "WHERE e.horarioEntrega >= :inicio AND e.horarioEntrega <= :fim " +
            "AND e.org.id = :orgId ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByHorarioEntregaBetweenAndOrgId(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim")    LocalDateTime fim,
            @Param("orgId")  Long orgId,
            Pageable pageable
    );

    /**
     * Lista todas as entregas de um consumidor na organização, com paginação e sem filtro de datas.
     *
     * @param consumidorId ID do consumidor
     * @param orgId        ID da organização
     * @param pageable     parâmetros de paginação
     * @return página de entregas do consumidor
     */
    @Query("SELECT e FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId AND e.org.id = :orgId " +
            "ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByConsumidorIdAndOrgId(
            @Param("consumidorId") Long consumidorId,
            @Param("orgId")        Long orgId,
            Pageable pageable
    );

    /**
     * Lista entregas de um consumidor em um intervalo de datas, com paginação.
     *
     * @param consumidorId ID do consumidor
     * @param inicio       data/hora inicial do intervalo (inclusive)
     * @param fim          data/hora final do intervalo (inclusive)
     * @param orgId        ID da organização
     * @param pageable     parâmetros de paginação
     * @return página de entregas do consumidor no intervalo
     */
    @Query("SELECT e FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId " +
            "AND e.horarioEntrega >= :inicio AND e.horarioEntrega <= :fim " +
            "AND e.org.id = :orgId ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByConsumidorIdAndHorarioEntregaBetweenAndOrgId(
            @Param("consumidorId") Long consumidorId,
            @Param("inicio")       LocalDateTime inicio,
            @Param("fim")          LocalDateTime fim,
            @Param("orgId")        Long orgId,
            Pageable pageable
    );

    // ======================================================
    // CONSULTAS POR PRODUTO COM PAGINAÇÃO E CONTAGEM
    // ======================================================

    /**
     * Lista entregas que contêm um determinado produto, filtradas pela organização, com paginação.
     *
     * @param produtoId ID do produto
     * @param orgId     ID da organização
     * @param pageable  parâmetros de paginação
     * @return página de entregas contendo o produto
     */
    @Query("SELECT e FROM Entrega e " +
            "WHERE e.produto.id = :produtoId AND e.org.id = :orgId " +
            "ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByProdutoIdAndOrgId(
            @Param("produtoId") Long produtoId,
            @Param("orgId")     Long orgId,
            Pageable pageable
    );

    // ======================================================
    // MÉTRICAS SIMPLES: totais diários e contagens
    // ======================================================

    /**
     * Soma dos valores de todas as entregas num dia específico.
     *
     * @param inicio data/hora inicial do dia (inclusive)
     * @param fim    data/hora final do dia (exclusive)
     * @param orgId  ID da organização
     * @return soma dos valores das entregas no dia; retorna 0 se não houver entregas
     */
    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM Entrega e " +
            "WHERE e.horarioEntrega >= :inicio AND e.horarioEntrega < :fim " +
            "AND e.org.id = :orgId")
    java.math.BigDecimal totalPorDia(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim")    LocalDateTime fim,
            @Param("orgId")  Long orgId
    );

    /**
     * Conta o total de entregas realizadas pela organização.
     *
     * @param orgId ID da organização
     * @return número total de entregas
     */
    @Query("SELECT COALESCE(COUNT(e), 0) FROM Entrega e WHERE e.org.id = :orgId")
    Integer totalEntregasRealizadas(@Param("orgId") Long orgId);

    /**
     * Conta o total de entregas realizadas por um consumidor na organização.
     *
     * @param consumidorId ID do consumidor
     * @param orgId        ID da organização
     * @return número de entregas do consumidor
     */
    @Query("SELECT COALESCE(COUNT(e), 0) FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId AND e.org.id = :orgId")
    Integer totalEntregasPorConsumidor(
            @Param("consumidorId") Long consumidorId,
            @Param("orgId")        Long orgId
    );

    /**
     * Conta o total de entregas que envolveram um produto específico na organização.
     *
     * @param produtoId ID do produto
     * @param orgId     ID da organização
     * @return número de entregas do produto
     */
    @Query("SELECT COALESCE(COUNT(e), 0) FROM Entrega e " +
            "WHERE e.produto.id = :produtoId AND e.org.id = :orgId")
    Integer totalEntregasPorProduto(
            @Param("produtoId") Long produtoId,
            @Param("orgId")     Long orgId
    );

    /**
     * Retorna o valor total (valor * quantidade) de uma única entrega
     * para a organização atual.
     */
    @Query("SELECT COALESCE(e.valor, 0) FROM Entrega e " +
            "WHERE e.id = :entregaId AND e.org.id = :orgId")
    java.math.BigDecimal totalPorEntrega(
            @Param("entregaId") Long entregaId,
            @Param("orgId")     Long orgId
    );

    /**
     * Retorna as entregas realizadas por um consumidor específico dentro de um intervalo anual para uma organização, com paginação.
     */
    @Query("SELECT e FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId " +
            "AND e.horarioEntrega >= :inicioAno AND e.horarioEntrega <= :fimAno " +
            "AND e.org.id = :orgId ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByConsumidorIdAndHorarioEntregaBetweenAndOrgIdAnnual(
            @Param("consumidorId") Long consumidorId,
            @Param("inicioAno") LocalDateTime inicioAno,
            @Param("fimAno") LocalDateTime fimAno,
            @Param("orgId") Long orgId,
            Pageable pageable
    );

    /**
     * Retorna as entregas realizadas por um consumidor específico dentro de um intervalo mensal para uma organização, com paginação.
     */
    @Query("SELECT e FROM Entrega e " +
            "WHERE e.consumidor.id = :consumidorId " +
            "AND e.horarioEntrega >= :inicioMes AND e.horarioEntrega <= :fimMes " +
            "AND e.org.id = :orgId ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByConsumidorIdAndHorarioEntregaBetweenAndOrgIdMonthly(
            @Param("consumidorId") Long consumidorId,
            @Param("inicioMes") LocalDateTime inicioMes,
            @Param("fimMes") LocalDateTime fimMes,
            @Param("orgId") Long orgId,
            Pageable pageable
    );

    /**
     * Retorna as entregas realizadas por uma organização dentro de um intervalo anual, com paginação.
     */
    @Query("SELECT e FROM Entrega e " +
            "WHERE e.horarioEntrega >= :inicioAno AND e.horarioEntrega <= :fimAno " +
            "AND e.org.id = :orgId ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByOrgIdAndHorarioEntregaBetweenAnnual(
            @Param("inicioAno") LocalDateTime inicioAno,
            @Param("fimAno") LocalDateTime fimAno,
            @Param("orgId") Long orgId,
            Pageable pageable
    );

    /**
     * Retorna as entregas realizadas por uma organização dentro de um intervalo mensal, com paginação.
     */
    @Query("SELECT e FROM Entrega e " +
            "WHERE e.horarioEntrega >= :inicioMes AND e.horarioEntrega <= :fimMes " +
            "AND e.org.id = :orgId ORDER BY e.horarioEntrega ASC")
    Page<Entrega> findByOrgIdAndHorarioEntregaBetweenMonthly(
            @Param("inicioMes") LocalDateTime inicioMes,
            @Param("fimMes") LocalDateTime fimMes,
            @Param("orgId") Long orgId,
            Pageable pageable
    );

}
