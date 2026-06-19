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

    @Column(name = "mercado_pago_payment_id", unique = true)
    private Long mercadoPagoPaymentId;

    @Column(name = "asaas_payment_id", unique = true)
    private String asaasPaymentId;

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

    /**
     * Timestamp da última atualização de status processada
     * Usado para prevenir regressão de status quando webhooks chegam fora de ordem
     * Sempre baseado no date_last_updated da API do Mercado Pago (fonte da verdade)
     */
    @Column(name = "last_status_update_at")
    private LocalDateTime lastStatusUpdateAt;

    /**
     * Indica se o pagamento está em disputa (chargeback)
     */
    @Column(name = "in_dispute", nullable = false)
    private Boolean inDispute = false;

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
     * Compatível com Mercado Pago
     */
    public enum PaymentStatus {
        /**
         * Pagamento aprovado (Mercado Pago: approved)
         */
        APPROVED,

        /**
         * Pagamento pendente
         */
        PENDING,

        /**
         * Pagamento rejeitado (Mercado Pago: rejected)
         */
        REJECTED,

        /**
         * Pagamento cancelado
         */
        CANCELLED,

        /**
         * Pagamento reembolsado
         */
        REFUNDED,

        /**
         * Pagamento estornado (chargeback)
         */
        CHARGED_BACK,

    }
}