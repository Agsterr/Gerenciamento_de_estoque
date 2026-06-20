package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.AcessoLogin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AcessoLoginRepository extends JpaRepository<AcessoLogin, Long> {

    Page<AcessoLogin> findAllByOrderByDataHoraDesc(Pageable pageable);

    @Query("""
            SELECT a FROM AcessoLogin a
            WHERE (:orgId IS NULL OR a.org.id = :orgId)
              AND a.dataHora >= :inicio
              AND a.dataHora < :fim
            ORDER BY a.dataHora DESC
            """)
    Page<AcessoLogin> findFiltrado(
            @Param("orgId") Long orgId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable);

    @Query(value = """
            SELECT CAST(EXTRACT(YEAR FROM data_hora) AS INTEGER) AS ano,
                   CAST(EXTRACT(MONTH FROM data_hora) AS INTEGER) AS mes,
                   CAST(EXTRACT(DAY FROM data_hora) AS INTEGER) AS dia,
                   COUNT(*) AS total
            FROM acessos_login
            WHERE (:orgId IS NULL OR org_id = :orgId)
            GROUP BY EXTRACT(YEAR FROM data_hora), EXTRACT(MONTH FROM data_hora), EXTRACT(DAY FROM data_hora)
            ORDER BY ano DESC, mes DESC, dia DESC
            """, nativeQuery = true)
    List<Object[]> agruparPorData(@Param("orgId") Long orgId);

    @Query("""
            SELECT a FROM AcessoLogin a
            WHERE (:orgId IS NULL OR a.org.id = :orgId)
              AND a.dataHora >= :inicio
              AND a.dataHora < :fim
            ORDER BY a.dataHora ASC
            """)
    List<AcessoLogin> findAllFiltrado(
            @Param("orgId") Long orgId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT COUNT(a) FROM AcessoLogin a
            WHERE (:orgId IS NULL OR a.org.id = :orgId)
              AND a.dataHora >= :inicio
              AND a.dataHora < :fim
            """)
    long countFiltrado(
            @Param("orgId") Long orgId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM AcessoLogin a
            WHERE (:orgId IS NULL OR a.org.id = :orgId)
              AND a.dataHora >= :inicio
              AND a.dataHora < :fim
            """)
    int deleteFiltrado(
            @Param("orgId") Long orgId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
