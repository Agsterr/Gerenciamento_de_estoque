package br.softsistem.Gerenciamento_de_estoque.model;

import java.time.LocalDateTime;
import java.util.List;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidade que representa uma assinatura de usuário
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Usuário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario user;

    @NotNull(message = "Plano é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "mercado_pago_card_id")
    private String mercadoPagoCardId;

    @Column(name = "mercado_pago_subscription_id")
    private String mercadoPagoSubscriptionId;

    @Column(name = "asaas_customer_id")
    private String asaasCustomerId;

    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;

    @Column(name = "asaas_subscription_id")
    private String asaasSubscriptionId;

    @Column(name = "payment_provider")
    private String paymentProvider = "ASAAS";

    /** Modo de pagamento Asaas: RECURRING, PIX ou BOLETO */
    @Column(name = "payment_mode")
    private String paymentMode;

    @Getter
    @Column(name = "trial_start")
    private LocalDateTime trialStart;

    @Getter
    @Setter
    @Column(name = "trial_end")
    private LocalDateTime trialEnd;

    @Getter
    @Setter
    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;

    @Setter
    @Getter
    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Setter
    @Getter
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Setter
    @Getter
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // URL do checkout (Checkout Pro) gerada pelo Mercado Pago
    @Setter
    @Getter
    @Column(name = "checkout_url", length = 1024)
    private String checkoutUrl;

    /**
     * Indica se o acesso ao serviço está bloqueado devido a chargeback
     * Quando true, o usuário não pode acessar o serviço mesmo que a assinatura
     * esteja ativa
     */
    @Column(name = "access_blocked", nullable = false)
    private Boolean accessBlocked = false;

    @Setter
    @Getter
    @Column(name = "trial_warning_sent")
    private Boolean trialWarningSent = false;

    @Setter
    @Getter
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Setter
    @Getter
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Setter
    @Getter
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Se é um novo trial, define as datas automaticamente
        if (status == SubscriptionStatus.TRIAL && trialStart == null) {
            trialStart = LocalDateTime.now();
            trialEnd = trialStart.plusDays(15);
            currentPeriodStart = trialStart;
            currentPeriodEnd = trialEnd;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica se a assinatura está em período de trial
     */
    public boolean isInTrial() {
        return status == SubscriptionStatus.TRIAL &&
                trialEnd != null &&
                LocalDateTime.now().isBefore(trialEnd);
    }

    /**
     * Verifica se o trial está próximo do fim (3 dias ou menos)
     */
    public boolean isTrialEndingSoon() {
        if (!isInTrial()) {
            return false;
        }
        return LocalDateTime.now().isAfter(trialEnd.minusDays(3));
    }

    /**
     * Verifica se a assinatura está ativa (trial ou paga)
     */
    public boolean isActive() {
        return status == SubscriptionStatus.TRIAL ||
                status == SubscriptionStatus.ACTIVE;
    }

    // Construtor padrão exigido pelo JPA
    public Subscription() {
    }

    // Getters e Setters explícitos (remoção do Lombok)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUser() {
        return user;
    }

    public void setUser(Usuario user) {
        this.user = user;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public String getMercadoPagoSubscriptionId() {
        return mercadoPagoSubscriptionId;
    }

    public void setMercadoPagoSubscriptionId(String mercadoPagoSubscriptionId) {
        this.mercadoPagoSubscriptionId = mercadoPagoSubscriptionId;
    }

    public String getMercadoPagoCardId() {
        return mercadoPagoCardId;
    }

    public Boolean getAccessBlocked() {
        return accessBlocked != null ? accessBlocked : false;
    }

    public void setAccessBlocked(Boolean accessBlocked) {
        this.accessBlocked = accessBlocked != null ? accessBlocked : false;
    }

    public String getAsaasCustomerId() {
        return asaasCustomerId;
    }

    public void setAsaasCustomerId(String asaasCustomerId) {
        this.asaasCustomerId = asaasCustomerId;
    }

    public String getAsaasPaymentId() {
        return asaasPaymentId;
    }

    public void setAsaasPaymentId(String asaasPaymentId) {
        this.asaasPaymentId = asaasPaymentId;
    }

    public String getAsaasSubscriptionId() {
        return asaasSubscriptionId;
    }

    public void setAsaasSubscriptionId(String asaasSubscriptionId) {
        this.asaasSubscriptionId = asaasSubscriptionId;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public void setTrialStart(LocalDateTime trialStart) {
        this.trialStart = trialStart;
    }

}
