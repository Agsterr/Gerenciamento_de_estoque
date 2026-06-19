package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.ChargebackHistory;
import br.softsistem.Gerenciamento_de_estoque.model.Payment;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para operações com histórico de chargebacks
 */
@Repository
public interface ChargebackHistoryRepository extends JpaRepository<ChargebackHistory, Long> {
    
    /**
     * Busca histórico de chargebacks por chargeback_id
     */
    List<ChargebackHistory> findByChargebackIdOrderByCreatedAtDesc(String chargebackId);
    
    /**
     * Busca histórico de chargebacks por payment
     */
    List<ChargebackHistory> findByPaymentOrderByCreatedAtDesc(Payment payment);
    
    /**
     * Busca histórico de chargebacks por subscription
     */
    List<ChargebackHistory> findBySubscriptionOrderByCreatedAtDesc(Subscription subscription);
    
    /**
     * Busca histórico de chargebacks por user_id
     */
    List<ChargebackHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Verifica se já existe registro para chargeback_id, payment_id e status
     */
    @Query("SELECT COUNT(c) > 0 FROM ChargebackHistory c WHERE c.chargebackId = :chargebackId AND c.payment.id = :paymentId AND c.status = :status")
    boolean existsByChargebackIdAndPaymentIdAndStatus(@Param("chargebackId") String chargebackId, 
                                                      @Param("paymentId") Long paymentId, 
                                                      @Param("status") String status);
    
    /**
     * Busca último chargeback por payment
     */
    Optional<ChargebackHistory> findFirstByPaymentOrderByCreatedAtDesc(Payment payment);
}







