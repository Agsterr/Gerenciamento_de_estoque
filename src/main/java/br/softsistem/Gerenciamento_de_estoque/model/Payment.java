package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa um pagamento de assinatura
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Assinatura é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;
    
    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;
    
    @Column(name = "stripe_invoice_id")
    private String stripeInvoiceId;
    
    @Column(name = "currency", length = 3)
    private String currency = "BRL";
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Enum que define os possíveis status de um pagamento
     */
    public enum PaymentStatus {
        /**
         * Pagamento pendente
         */
        PENDING,
        
        /**
         * Pagamento processando
         */
        PROCESSING,
        
        /**
         * Pagamento bem-sucedido
         */
        SUCCEEDED,
        
        /**
         * Pagamento falhou
         */
        FAILED,
        
        /**
         * Pagamento cancelado
         */
        CANCELED,
        
        /**
         * Pagamento reembolsado
         */
        REFUNDED
    }
}