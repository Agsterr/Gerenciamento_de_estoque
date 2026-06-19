package br.softsistem.Gerenciamento_de_estoque.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;

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
     * Busca assinatura por ID do Mercado Pago (preapproval_id)
     */
    Optional<Subscription> findByMercadoPagoSubscriptionId(String mercadoPagoSubscriptionId);

    Optional<Subscription> findByAsaasPaymentId(String asaasPaymentId);

    Optional<Subscription> findByAsaasSubscriptionId(String asaasSubscriptionId);

    default boolean hasActiveOrTrialSubscription(Usuario user) {
        return hasActiveSubscription(user);
    }

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
     * Busca todas as assinaturas do usuário por ID
     */
    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Busca todas as assinaturas do usuário
     */
    List<Subscription> findByUserOrderByCreatedAtDesc(Usuario user);

    /**
     * Busca assinatura por user ID
     */
    Optional<Subscription> findByUserId(Long userId);

    /**
     * Busca assinatura por user ID e status exato
     */
    Optional<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    /**
     * Busca assinatura ativa por user ID e status
     */
    Optional<Subscription> findByUserIdAndStatusIn(Long userId, List<SubscriptionStatus> statuses);

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

    /**
     * Busca assinaturas canceladas antes de uma data específica
     */
    List<Subscription> findByStatusAndEndedAtBefore(SubscriptionStatus status, LocalDateTime endDate);

    /**
     * Busca assinaturas canceladas entre duas datas
     */
    List<Subscription> findByStatusAndEndedAtBetween(SubscriptionStatus status, LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * Busca assinaturas criadas entre duas datas
     */
    List<Subscription> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
