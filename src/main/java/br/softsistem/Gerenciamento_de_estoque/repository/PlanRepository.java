package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para operações com planos de assinatura
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * Busca planos ativos
     */
    List<Plan> findByIsActiveTrue();

    /**
     * Busca plano por tipo
     */
    Optional<Plan> findByTypeAndIsActiveTrue(PlanType type);

    /**
     * Busca plano por tipo (incluindo inativos)
     */
    Optional<Plan> findByType(PlanType type);

    /**
     * Busca planos ordenados por preço
     */
    @Query("SELECT p FROM Plan p WHERE p.isActive = true ORDER BY p.price ASC")
    List<Plan> findActivePlansOrderByPrice();

    /**
     * Verifica se existe plano com o nome (apenas ativos)
     */
    boolean existsByNameAndIsActiveTrue(String name);

    /**
     * Verifica se existe plano com o nome (qualquer status)
     */
    boolean existsByName(String name);

    Optional<Plan> findByMercadoPagoPreapprovalPlanId(String mercadoPagoPreapprovalPlanId);

    boolean existsByMercadoPagoPreapprovalPlanId(String mercadoPagoPreapprovalPlanId);

    Optional<Plan> findByMercadoPagoPreapprovalPlanIdAndIsActiveTrue(String mercadoPagoPreapprovalPlanId);

    List<Plan> findByMercadoPagoPreapprovalPlanIdIsNotNull();
}
