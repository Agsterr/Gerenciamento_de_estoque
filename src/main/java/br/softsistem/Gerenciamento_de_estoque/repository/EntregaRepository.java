package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    // Total de entregas por dia (soma dos valores) filtrando pela organização
    @Query("SELECT SUM(e.valor) FROM Entrega e WHERE e.horarioEntrega = :dia AND e.org.id = :orgId")
    BigDecimal totalPorDia(LocalDate dia, Long orgId);

    // Total semanal (soma dos valores) filtrando pela organização
    @Query("SELECT SUM(e.valor) FROM Entrega e WHERE e.horarioEntrega BETWEEN :inicioSemana AND :fimSemana AND e.org.id = :orgId")
    BigDecimal totalSemanal(LocalDate inicioSemana, LocalDate fimSemana, Long orgId);

    // Total mensal (soma dos valores) para o mês e ano fornecidos, filtrando pela organização
    @Query("SELECT SUM(e.valor) FROM Entrega e WHERE FUNCTION('MONTH', e.horarioEntrega) = :mes AND FUNCTION('YEAR', e.horarioEntrega) = :ano AND e.org.id = :orgId")
    BigDecimal totalMensal(int mes, int ano, Long orgId);

    // Total de entregas de um consumidor específico (por ID), filtrando pela organização
    @Query("SELECT SUM(e.valor) FROM Entrega e WHERE e.consumidor.id = :consumidorId AND e.org.id = :orgId")
    BigDecimal totalPorConsumidor(Long consumidorId, Long orgId);

    // Total de entregas feitas no mês corrente, filtrando pela organização
    @Query("SELECT SUM(e.valor) FROM Entrega e WHERE FUNCTION('MONTH', e.horarioEntrega) = FUNCTION('MONTH', CURRENT_DATE) AND FUNCTION('YEAR', e.horarioEntrega) = FUNCTION('YEAR', CURRENT_DATE) AND e.org.id = :orgId")
    BigDecimal totalDoMesAtual(Long orgId);

    // Total semanal por consumidor, filtrando pela organização
    @Query("SELECT SUM(e.valor) FROM Entrega e WHERE e.consumidor.id = :consumidorId AND e.horarioEntrega BETWEEN :inicioSemana AND :fimSemana AND e.org.id = :orgId")
    BigDecimal totalSemanalPorConsumidor(Long consumidorId, LocalDate inicioSemana, LocalDate fimSemana, Long orgId);

    // Listar todas as entregas de uma organização específica
    @Query("SELECT e FROM Entrega e WHERE e.org.id = :orgId")
    List<Entrega> findByOrgId(Long orgId);

    // Total anual (soma dos valores) para o ano fornecido, filtrando pela organização
    @Query("SELECT SUM(e.valor) FROM Entrega e WHERE FUNCTION('YEAR', e.horarioEntrega) = :ano AND e.org.id = :orgId")
    BigDecimal totalAnual(int ano, Long orgId);

    // Total anual de entregas feitas por um consumidor específico, filtrando pela organização
    @Query("SELECT SUM(e.valor) FROM Entrega e WHERE e.consumidor.id = :consumidorId AND FUNCTION('YEAR', e.horarioEntrega) = :ano AND e.org.id = :orgId")
    BigDecimal totalAnualPorConsumidor(Long consumidorId, int ano, Long orgId);

}
