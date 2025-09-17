package br.softsistem.Gerenciamento_de_estoque.dto.subscriptionDto;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;

import java.time.LocalDateTime;

/**
 * DTO para exposição de dados de Subscription na API.
 * Sem Lombok para evitar dependência do processador de anotações.
 */
public class SubscriptionDto {

    private Long id;
    private String planName;
    private PlanType planType;
    private SubscriptionStatus status;
    private LocalDateTime trialStart;
    private LocalDateTime trialEnd;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private Boolean isInTrial;
    private Boolean isTrialEndingSoon;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public SubscriptionDto() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public PlanType getPlanType() { return planType; }
    public void setPlanType(PlanType planType) { this.planType = planType; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public LocalDateTime getTrialStart() { return trialStart; }
    public void setTrialStart(LocalDateTime trialStart) { this.trialStart = trialStart; }

    public LocalDateTime getTrialEnd() { return trialEnd; }
    public void setTrialEnd(LocalDateTime trialEnd) { this.trialEnd = trialEnd; }

    public LocalDateTime getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(LocalDateTime currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }

    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }

    public Boolean getIsInTrial() { return isInTrial; }
    public void setIsInTrial(Boolean isInTrial) { this.isInTrial = isInTrial; }

    public Boolean getIsTrialEndingSoon() { return isTrialEndingSoon; }
    public void setIsTrialEndingSoon(Boolean isTrialEndingSoon) { this.isTrialEndingSoon = isTrialEndingSoon; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}