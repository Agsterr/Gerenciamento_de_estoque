package br.softsistem.Gerenciamento_de_estoque.dto;

import br.softsistem.Gerenciamento_de_estoque.model.Subscription;

public final class SubscriptionMapper {
    private SubscriptionMapper() {}

    public static SubscriptionDto toDto(Subscription subscription) {
        return toDto(subscription, 15);
    }

    public static SubscriptionDto toDto(Subscription subscription, int trialDays) {
        if (subscription == null) return null;
        SubscriptionDto dto = new SubscriptionDto(subscription);
        dto.enrichTrialMetrics(trialDays);
        if (subscription.getCheckoutUrl() != null) {
            dto.setPaymentUrl(subscription.getCheckoutUrl());
        }
        if (subscription.getAsaasPaymentId() != null) {
            dto.setAsaasPaymentId(subscription.getAsaasPaymentId());
            boolean awaitingPayment = subscription.getStatus() == br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus.TRIAL
                    || subscription.getStatus() == br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus.INCOMPLETE;
            dto.setPendingPayment(awaitingPayment);
        }
        return dto;
    }
}
