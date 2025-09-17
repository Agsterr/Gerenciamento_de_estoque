package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operações com assinaturas
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    /**
     * Busca assinatura ativa do usuário
     */
    @Query("SELECT s FROM Subscription s WHERE s.user = :user AND s.status IN ('TRIAL', 'ACTIVE') ORDER BY s.createdAt DESC")
    Optional<Subscription> findActiveSubscriptionByUser(@Param("user") Usuario user);
    
    /**
     * Busca assinatura por ID do Stripe
     */
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    /**
     * Busca assinaturas por status
     */
    List<Subscription> findByStatus(SubscriptionStatus status);
    
    /**
     * Busca assinaturas em trial que estão próximas do fim
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEnd <= :endDate AND s.trialWarningSent = false")
    List<Subscription> findTrialsEndingSoon(@Param("endDate") LocalDateTime endDate);
    
    /**
     * Busca assinaturas em trial expiradas
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEnd < :now")
    List<Subscription> findExpiredTrials(@Param("now") LocalDateTime now);
    
    /**
     * Busca todas as assinaturas do usuário
     */
    List<Subscription> findByUserOrderByCreatedAtDesc(Usuario user);
    
    /**
     * Busca assinaturas por customer ID do Stripe
     */
    List<Subscription> findByStripeCustomerId(String stripeCustomerId);
    
    /**
     * Verifica se usuário tem assinatura ativa
     */
    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.user = :user AND s.status IN ('TRIAL', 'ACTIVE')")
    boolean hasActiveSubscription(@Param("user") Usuario user);
    
    /**
     * Busca assinaturas que precisam ser renovadas
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.currentPeriodEnd <= :endDate")
    List<Subscription> findSubscriptionsToRenew(@Param("endDate") LocalDateTime endDate);
}