package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Payment;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operações com pagamentos
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * Busca pagamentos por assinatura
     */
    List<Payment> findBySubscriptionOrderByCreatedAtDesc(Subscription subscription);
    
    /**
     * Busca pagamento por ID do payment intent do Stripe
     */
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    
    /**
     * Busca pagamento por ID da invoice do Stripe
     */
    Optional<Payment> findByStripeInvoiceId(String stripeInvoiceId);
    
    /**
     * Busca pagamentos por status
     */
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    /**
     * Busca pagamentos bem-sucedidos de uma assinatura
     */
    @Query("SELECT p FROM Payment p WHERE p.subscription = :subscription AND p.status = 'SUCCEEDED' ORDER BY p.paidAt DESC")
    List<Payment> findSuccessfulPaymentsBySubscription(@Param("subscription") Subscription subscription);
    
    /**
     * Busca pagamentos falhados de uma assinatura
     */
    @Query("SELECT p FROM Payment p WHERE p.subscription = :subscription AND p.status = 'FAILED' ORDER BY p.failedAt DESC")
    List<Payment> findFailedPaymentsBySubscription(@Param("subscription") Subscription subscription);
    
    /**
     * Busca último pagamento bem-sucedido de uma assinatura
     */
    @Query("SELECT p FROM Payment p WHERE p.subscription = :subscription AND p.status = 'SUCCEEDED' ORDER BY p.paidAt DESC LIMIT 1")
    Optional<Payment> findLastSuccessfulPayment(@Param("subscription") Subscription subscription);
    
    /**
     * Busca pagamentos em um período
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Conta pagamentos bem-sucedidos de uma assinatura
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.subscription = :subscription AND p.status = 'SUCCEEDED'")
    long countSuccessfulPaymentsBySubscription(@Param("subscription") Subscription subscription);
}