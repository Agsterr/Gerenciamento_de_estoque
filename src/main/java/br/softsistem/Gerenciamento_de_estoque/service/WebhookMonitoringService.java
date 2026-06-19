package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.repository.FailedWebhookEventRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service para monitoramento e alertas de webhooks
 * 
 * Implementa:
 * - Métricas básicas (total recebido, sucesso, falha, tempo médio)
 * - Alertas críticos (taxa de falha, timeout, chargeback)
 * - Canais de alerta (email, webhook)
 */
@Service
public class WebhookMonitoringService {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookMonitoringService.class);
    
    private final WebhookEventRepository webhookEventRepository;
    private final FailedWebhookEventRepository failedWebhookEventRepository;
    private final WebhookAlertService alertService;
    
    // Métricas essenciais em memória
    private final AtomicLong totalWebhooksReceived = new AtomicLong(0);
    private final AtomicLong totalWebhooksProcessed = new AtomicLong(0);
    private final AtomicLong totalWebhooksFailed = new AtomicLong(0);
    private final AtomicLong consecutiveWebhooksFailed = new AtomicLong(0);
    
    @Value("${webhook.monitoring.consecutive_fail_threshold:5}")
    private int consecutiveFailThreshold;
    
    @Value("${webhook.monitoring.unprocessed_alert_minutes:5}")
    private int unprocessedAlertMinutes;
    
    @Autowired
    public WebhookMonitoringService(
            WebhookEventRepository webhookEventRepository,
            FailedWebhookEventRepository failedWebhookEventRepository,
            WebhookAlertService alertService) {
        this.webhookEventRepository = webhookEventRepository;
        this.failedWebhookEventRepository = failedWebhookEventRepository;
        this.alertService = alertService;
    }
    
    /**
     * Registra recebimento de webhook
     */
    public void recordWebhookReceived(String eventType) {
        totalWebhooksReceived.incrementAndGet();
        log.info("Webhook recebido: {}", eventType);
    }
    
    /**
     * Registra processamento bem-sucedido
     */
    public void recordWebhookProcessed(String eventType) {
        totalWebhooksProcessed.incrementAndGet();
        consecutiveWebhooksFailed.set(0);
        log.info("Webhook processado com sucesso: {}", eventType);
    }
    
    /**
     * Registra falha no processamento
     */
    public void recordWebhookFailed(String eventType) {
        totalWebhooksFailed.incrementAndGet();
        consecutiveWebhooksFailed.incrementAndGet();
        log.warn("Falha ao processar webhook: {}", eventType);
    }
    
    /**
     * Registra chargeback detectado (alerta imediato)
     */
    public void recordChargebackDetected(String paymentId, String userId) {
        log.error("🚨 CHARGEBACK DETECTADO - payment_id={}, external_reference={}", paymentId, userId);
        alertService.sendChargebackAlert(paymentId, userId);
        logStructured("webhook.chargeback.detected", Map.of(
            "payment_id", paymentId,
            "external_reference", userId != null ? userId : "unknown",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    /**
     * Obtém métricas atuais
     */
    public WebhookMetrics getMetrics() {
        long totalReceived = totalWebhooksReceived.get();
        long totalProcessed = totalWebhooksProcessed.get();
        long totalFailed = totalWebhooksFailed.get();
        
        double successRate = totalReceived > 0 ? (double) totalProcessed / totalReceived * 100 : 0.0;
        double failureRate = totalReceived > 0 ? (double) totalFailed / totalReceived * 100 : 0.0;
        
        // Contar eventos não processados do banco
        long unprocessedCount = webhookEventRepository.countByProcessedFalse();
        long failedEventsCount = failedWebhookEventRepository.count();
        
        return new WebhookMetrics(
            totalReceived,
            totalProcessed,
            totalFailed,
            successRate,
            failureRate,
            unprocessedCount,
            failedEventsCount
        );
    }
    
    /**
     * Job agendado para verificar alertas críticos
     * Executa a cada 5 minutos
     */
    @Scheduled(fixedDelay = 300000)
    public void checkCriticalAlerts() {
        try {
            log.debug("Verificando alertas críticos de webhooks...");
            
            WebhookMetrics metrics = getMetrics();
            
            if (consecutiveWebhooksFailed.get() >= consecutiveFailThreshold) {
                String message = String.format(
                    "Falhas consecutivas de webhooks: %d (limite: %d)",
                    consecutiveWebhooksFailed.get(), consecutiveFailThreshold
                );
                log.warn(message);
                alertService.sendAlert("Falhas Consecutivas de Webhooks", message, "WARNING");
            }
            
            long unprocessedCount = metrics.getUnprocessedCount();
            if (unprocessedCount > 0) {
                LocalDateTime threshold = LocalDateTime.now().minusMinutes(unprocessedAlertMinutes);
                long oldUnprocessed = webhookEventRepository.countByProcessedFalseAndCreatedAtBefore(threshold);
                
                if (oldUnprocessed > 0) {
                    String message = String.format(
                        "%d webhooks não processados há mais de %d minutos",
                        oldUnprocessed, unprocessedAlertMinutes
                    );
                    log.warn(message);
                    alertService.sendAlert("Webhooks Não Processados", message, "WARNING");
                }
            }
            
        } catch (Exception e) {
            log.error("Erro ao verificar alertas críticos: {}", e.getMessage());
        }
    }
    
    /**
     * Log estruturado
     */
    private void logStructured(String event, Map<String, String> fields) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[WEBHOOK-MONITORING] ");
        logMessage.append("event=").append(event);
        
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            logMessage.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        log.info(logMessage.toString());
    }
    
    /**
     * Classe para armazenar métricas
     */
    public static class WebhookMetrics {
        private final long totalReceived;
        private final long totalProcessed;
        private final long totalFailed;
        private final double successRate;
        private final double failureRate;
        private final long unprocessedCount;
        private final long failedEventsCount;
        
        public WebhookMetrics(long totalReceived, long totalProcessed, long totalFailed,
                            double successRate, double failureRate,
                            long unprocessedCount, long failedEventsCount) {
            this.totalReceived = totalReceived;
            this.totalProcessed = totalProcessed;
            this.totalFailed = totalFailed;
            this.successRate = successRate;
            this.failureRate = failureRate;
            this.unprocessedCount = unprocessedCount;
            this.failedEventsCount = failedEventsCount;
        }
        
        // Getters
        public long getTotalReceived() { return totalReceived; }
        public long getTotalProcessed() { return totalProcessed; }
        public long getTotalFailed() { return totalFailed; }
        public double getSuccessRate() { return successRate; }
        public double getFailureRate() { return failureRate; }
        public long getUnprocessedCount() { return unprocessedCount; }
        public long getFailedEventsCount() { return failedEventsCount; }
    }
}







