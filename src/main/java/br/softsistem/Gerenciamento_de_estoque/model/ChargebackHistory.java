package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa o histórico de chargebacks e reclamações
 * Mantém registro completo de todas as alterações de status de chargebacks
 */
@Entity
@Table(name = "chargeback_history",
       uniqueConstraints = @UniqueConstraint(
           name = "idx_chargeback_history_unique",
           columnNames = {"chargeback_id", "payment_id", "status"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargebackHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID do chargeback no Mercado Pago
     */
    @Column(name = "chargeback_id", nullable = false, length = 255)
    private String chargebackId;
    
    /**
     * Pagamento relacionado
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    /**
     * Assinatura relacionada
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
    
    /**
     * Usuário relacionado (para facilitar consultas)
     */
    @Column(name = "user_id")
    private Long userId;
    
    /**
     * Status do chargeback (created, updated, resolved, etc.)
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status;
    
    /**
     * Ação do evento (chargeback.created, chargeback.updated, etc.)
     */
    @Column(name = "action", nullable = false, length = 100)
    private String action;
    
    /**
     * Motivo do chargeback
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    /**
     * Valor do chargeback
     */
    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}







