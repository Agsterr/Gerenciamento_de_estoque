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
     * Busca plano por ID do produto Stripe
     */
    Optional<Plan> findByStripeProductId(String stripeProductId);
    
    /**
     * Busca plano por ID do preço Stripe
     */
    Optional<Plan> findByStripePriceId(String stripePriceId);
    
    /**
     * Busca planos ordenados por preço
     */
    @Query("SELECT p FROM Plan p WHERE p.isActive = true ORDER BY p.price ASC")
    List<Plan> findActivePlansOrderByPrice();
    
    /**
     * Verifica se existe plano com o nome
     */
    boolean existsByNameAndIsActiveTrue(String name);
}