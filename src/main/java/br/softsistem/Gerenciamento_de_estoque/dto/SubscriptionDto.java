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
    private String stripeSubscriptionId;
    private String stripeCustomerId;
    
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
    
    // Construtor padrão
    public SubscriptionDto() {}
    
    // Construtor a partir da entidade
    public SubscriptionDto(Subscription subscription) {
        this.id = subscription.getId();
        this.status = subscription.getStatus();
        this.stripeSubscriptionId = subscription.getStripeSubscriptionId();
        this.stripeCustomerId = subscription.getStripeCustomerId();
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
            this.planType = subscription.getPlan().getType() != null ? 
                subscription.getPlan().getType().name() : null;
            this.planPrice = subscription.getPlan().getPrice();
            
            // Recursos do plano
            this.maxUsers = subscription.getPlan().getMaxUsers();
            this.maxProducts = subscription.getPlan().getMaxProducts();
            this.maxOrganizations = subscription.getPlan().getMaxOrganizations();
            this.hasReports = subscription.getPlan().getHasReports();
            this.hasAdvancedAnalytics = subscription.getPlan().getHasAdvancedAnalytics();
            this.hasApiAccess = subscription.getPlan().getHasApiAccess();
        }
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    
    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
    
    public BigDecimal getPlanPrice() { return planPrice; }
    public void setPlanPrice(BigDecimal planPrice) { this.planPrice = planPrice; }
    
    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }
    
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }
    
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    
    public LocalDateTime getTrialStart() { return trialStart; }
    public void setTrialStart(LocalDateTime trialStart) { this.trialStart = trialStart; }
    
    public LocalDateTime getTrialEnd() { return trialEnd; }
    public void setTrialEnd(LocalDateTime trialEnd) { this.trialEnd = trialEnd; }
    
    public LocalDateTime getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(LocalDateTime currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }
    
    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
    
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public void setCanceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; }
    
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Boolean getTrialWarningSent() { return trialWarningSent; }
    public void setTrialWarningSent(Boolean trialWarningSent) { this.trialWarningSent = trialWarningSent; }
    
    public Boolean getIsInTrial() { return isInTrial; }
    public void setIsInTrial(Boolean isInTrial) { this.isInTrial = isInTrial; }
    
    public Boolean getIsTrialEndingSoon() { return isTrialEndingSoon; }
    public void setIsTrialEndingSoon(Boolean isTrialEndingSoon) { this.isTrialEndingSoon = isTrialEndingSoon; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
    
    public Integer getMaxProducts() { return maxProducts; }
    public void setMaxProducts(Integer maxProducts) { this.maxProducts = maxProducts; }
    
    public Integer getMaxOrganizations() { return maxOrganizations; }
    public void setMaxOrganizations(Integer maxOrganizations) { this.maxOrganizations = maxOrganizations; }
    
    public Boolean getHasReports() { return hasReports; }
    public void setHasReports(Boolean hasReports) { this.hasReports = hasReports; }
    
    public Boolean getHasAdvancedAnalytics() { return hasAdvancedAnalytics; }
    public void setHasAdvancedAnalytics(Boolean hasAdvancedAnalytics) { this.hasAdvancedAnalytics = hasAdvancedAnalytics; }
    
    public Boolean getHasApiAccess() { return hasApiAccess; }
    public void setHasApiAccess(Boolean hasApiAccess) { this.hasApiAccess = hasApiAccess; }
}