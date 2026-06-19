package br.softsistem.Gerenciamento_de_estoque.dto;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferência de dados de assinatura
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionDto {

    private Long id;
    private Long userId;
    private String userEmail;
    private String userName;
    private Long planId;
    private String planName;
    private String planType;
    private BigDecimal planPrice;
    private SubscriptionStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime trialStart;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime trialEnd;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime currentPeriodStart;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime currentPeriodEnd;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime canceledAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private Boolean trialWarningSent;
    private Boolean isInTrial;
    private Boolean isTrialEndingSoon;
    private Boolean isActive;

    // Recursos do plano
    private Integer maxUsers;
    private Integer maxProducts;
    private Integer maxOrganizations;
    private Boolean hasReports;
    private Boolean hasAdvancedAnalytics;
    private Boolean hasApiAccess;

    /** ID da assinatura no Mercado Pago (preapproval_id), usado para cancelamento */
    private String preapprovalId;

    /** URL de pagamento Asaas (invoiceUrl) */
    private String paymentUrl;

    private String paymentProvider;

    private String asaasPaymentId;

    /** Modo Asaas: RECURRING, PIX ou BOLETO */
    private String paymentMode;

    /** Cobrança Asaas gerada, aguardando confirmação (durante trial) */
    private Boolean pendingPayment;

    /** Dias restantes no período de teste */
    private Long trialDaysRemaining;

    /** Dias já utilizados no teste */
    private Long trialDaysElapsed;

    /** Total de dias do teste gratuito */
    private Integer trialDaysTotal;

    // Construtor padrão
    public SubscriptionDto() {
    }

    // Construtor a partir da entidade
    public SubscriptionDto(Subscription subscription) {
        this.id = subscription.getId();
        this.status = subscription.getStatus();
        this.trialStart = subscription.getTrialStart();
        this.trialEnd = subscription.getTrialEnd();
        this.currentPeriodStart = subscription.getCurrentPeriodStart();
        this.currentPeriodEnd = subscription.getCurrentPeriodEnd();
        this.canceledAt = subscription.getCanceledAt();
        this.endedAt = subscription.getEndedAt();
        this.createdAt = subscription.getCreatedAt();
        this.updatedAt = subscription.getUpdatedAt();
        this.trialWarningSent = subscription.getTrialWarningSent();

        // Métodos de conveniência
        this.isInTrial = subscription.isInTrial();
        this.isTrialEndingSoon = subscription.isTrialEndingSoon();
        this.isActive = subscription.isActive();

        // Dados do usuário
        if (subscription.getUser() != null) {
            this.userId = subscription.getUser().getId();
            this.userEmail = subscription.getUser().getEmail();
            this.userName = subscription.getUser().getUsername();
        }

        // Dados do plano
        if (subscription.getPlan() != null) {
            this.planId = subscription.getPlan().getId();
            this.planName = subscription.getPlan().getName();
            this.planType = subscription.getPlan().getType() != null ? subscription.getPlan().getType().name() : null;
            this.planPrice = subscription.getPlan().getPrice();

            // Recursos do plano
            this.maxUsers = subscription.getPlan().getMaxUsers();
            this.maxProducts = subscription.getPlan().getMaxProducts();
            this.maxOrganizations = subscription.getPlan().getMaxOrganizations();
            this.hasReports = subscription.getPlan().getHasReports();
            this.hasAdvancedAnalytics = subscription.getPlan().getHasAdvancedAnalytics();
            this.hasApiAccess = subscription.getPlan().getHasApiAccess();
        }
        this.preapprovalId = subscription.getMercadoPagoSubscriptionId();
        this.paymentProvider = subscription.getPaymentProvider();
        this.paymentMode = subscription.getPaymentMode();
    }

    public void enrichTrialMetrics() {
        enrichTrialMetrics(15);
    }

    public void enrichTrialMetrics(int configuredTrialDays) {
        LocalDateTime start = trialStart;
        LocalDateTime end = trialEnd;

        if (SubscriptionStatus.TRIAL.equals(status) && (start == null || end == null)) {
            start = start != null ? start : (createdAt != null ? createdAt : LocalDateTime.now());
            end = end != null ? end : start.plusDays(configuredTrialDays);
            this.trialStart = start;
            this.trialEnd = end;
            this.isInTrial = LocalDateTime.now().isBefore(end);
        } else if (SubscriptionStatus.TRIAL.equals(status) && (isInTrial == null || !isInTrial)) {
            this.isInTrial = end != null && LocalDateTime.now().isBefore(end);
        }

        if (start == null || end == null) {
            this.trialDaysTotal = configuredTrialDays;
            return;
        }

        long total = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
        this.trialDaysTotal = total > 0 ? (int) total : configuredTrialDays;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isBefore(start)) {
            this.trialDaysElapsed = 0L;
            this.trialDaysRemaining = (long) trialDaysTotal;
            return;
        }
        long elapsed = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), now.toLocalDate());
        if (elapsed < 0) elapsed = 0;
        this.trialDaysElapsed = elapsed;
        if (Boolean.TRUE.equals(isInTrial)) {
            long remaining = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), end.toLocalDate());
            this.trialDaysRemaining = Math.max(0, remaining);
        } else {
            this.trialDaysRemaining = 0L;
        }
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public String getAsaasPaymentId() {
        return asaasPaymentId;
    }

    public void setAsaasPaymentId(String asaasPaymentId) {
        this.asaasPaymentId = asaasPaymentId;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public Boolean getPendingPayment() {
        return pendingPayment;
    }

    public void setPendingPayment(Boolean pendingPayment) {
        this.pendingPayment = pendingPayment;
    }

    public Long getTrialDaysRemaining() {
        return trialDaysRemaining;
    }

    public void setTrialDaysRemaining(Long trialDaysRemaining) {
        this.trialDaysRemaining = trialDaysRemaining;
    }

    public Long getTrialDaysElapsed() {
        return trialDaysElapsed;
    }

    public void setTrialDaysElapsed(Long trialDaysElapsed) {
        this.trialDaysElapsed = trialDaysElapsed;
    }

    public Integer getTrialDaysTotal() {
        return trialDaysTotal;
    }

    public void setTrialDaysTotal(Integer trialDaysTotal) {
        this.trialDaysTotal = trialDaysTotal;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public BigDecimal getPlanPrice() {
        return planPrice;
    }

    public void setPlanPrice(BigDecimal planPrice) {
        this.planPrice = planPrice;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public LocalDateTime getTrialStart() {
        return trialStart;
    }

    public void setTrialStart(LocalDateTime trialStart) {
        this.trialStart = trialStart;
    }

    public LocalDateTime getTrialEnd() {
        return trialEnd;
    }

    public void setTrialEnd(LocalDateTime trialEnd) {
        this.trialEnd = trialEnd;
    }

    public LocalDateTime getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public void setCurrentPeriodStart(LocalDateTime currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public LocalDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getTrialWarningSent() {
        return trialWarningSent;
    }

    public void setTrialWarningSent(Boolean trialWarningSent) {
        this.trialWarningSent = trialWarningSent;
    }

    public Boolean getIsInTrial() {
        return isInTrial;
    }

    public void setIsInTrial(Boolean isInTrial) {
        this.isInTrial = isInTrial;
    }

    public Boolean getIsTrialEndingSoon() {
        return isTrialEndingSoon;
    }

    public void setIsTrialEndingSoon(Boolean isTrialEndingSoon) {
        this.isTrialEndingSoon = isTrialEndingSoon;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxProducts() {
        return maxProducts;
    }

    public void setMaxProducts(Integer maxProducts) {
        this.maxProducts = maxProducts;
    }

    public Integer getMaxOrganizations() {
        return maxOrganizations;
    }

    public void setMaxOrganizations(Integer maxOrganizations) {
        this.maxOrganizations = maxOrganizations;
    }

    public Boolean getHasReports() {
        return hasReports;
    }

    public void setHasReports(Boolean hasReports) {
        this.hasReports = hasReports;
    }

    public Boolean getHasAdvancedAnalytics() {
        return hasAdvancedAnalytics;
    }

    public void setHasAdvancedAnalytics(Boolean hasAdvancedAnalytics) {
        this.hasAdvancedAnalytics = hasAdvancedAnalytics;
    }

    public Boolean getHasApiAccess() {
        return hasApiAccess;
    }

    public void setHasApiAccess(Boolean hasApiAccess) {
        this.hasApiAccess = hasApiAccess;
    }

    public String getPreapprovalId() {
        return preapprovalId;
    }

    public void setPreapprovalId(String preapprovalId) {
        this.preapprovalId = preapprovalId;
    }
}