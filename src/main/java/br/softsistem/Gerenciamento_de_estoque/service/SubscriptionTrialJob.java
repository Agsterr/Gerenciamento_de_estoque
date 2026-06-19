package br.softsistem.Gerenciamento_de_estoque.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionTrialJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionTrialJob.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionTrialJob(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void sendTrialEndingAlerts() {
        log.info("Executando job de alertas de fim de trial");
        subscriptionService.sendTrialEndingAlerts();
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void processExpiredTrials() {
        log.info("Executando job de trials expirados");
        subscriptionService.processExpiredTrials();
    }
}
