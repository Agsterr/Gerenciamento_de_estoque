package br.softsistem.Gerenciamento_de_estoque.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service para envio de alertas de webhooks
 * 
 * Suporta:
 * - Email
 * - Webhook (Slack, Teams, etc.)
 */
@Service
public class WebhookAlertService {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookAlertService.class);
    
    @Value("${webhook.alert.email.enabled:true}")
    private boolean emailEnabled;
    
    @Value("${webhook.alert.webhook.enabled:false}")
    private boolean webhookEnabled;
    
    @Value("${webhook.alert.webhook.url:}")
    private String webhookUrl;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    /**
     * Envia alerta de chargeback (alerta imediato)
     */
    public void sendChargebackAlert(String paymentId, String userId) {
        String subject = "🚨 Chargeback detectado";
        String message = String.format(
            "Evento de chargeback:\n\n" +
            "payment_id: %s\n" +
            "external_reference: %s\n" +
            "status: %s\n" +
            "timestamp: %s",
            paymentId,
            userId != null ? userId : "N/A",
            "chargeback",
            java.time.LocalDateTime.now()
        );
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payment_id", paymentId);
        metadata.put("external_reference", userId);
        metadata.put("status", "chargeback");
        sendAlert(subject, message, "CRITICAL", metadata);
    }

    public void sendPaymentApprovedAlert(String paymentId, String userId, BigDecimal amount) {
        String subject = "✅ Pagamento aprovado";
        String message = String.format(
            "Pagamento aprovado:\n\n" +
            "payment_id: %s\n" +
            "external_reference: %s\n" +
            "status: %s\n" +
            "amount: %s\n" +
            "timestamp: %s",
            paymentId,
            userId != null ? userId : "N/A",
            "approved",
            amount != null ? amount.toPlainString() : "0",
            java.time.LocalDateTime.now()
        );
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payment_id", paymentId);
        metadata.put("external_reference", userId);
        metadata.put("status", "approved");
        metadata.put("amount", amount != null ? amount.toPlainString() : "0");
        sendAlert(subject, message, "INFO", metadata);
    }

    public void sendPaymentRejectedAlert(String paymentId, String userId, String reason) {
        String subject = "⚠️ Pagamento recusado";
        String message = String.format(
            "Pagamento recusado:\n\n" +
            "payment_id: %s\n" +
            "external_reference: %s\n" +
            "status: %s\n" +
            "reason: %s\n" +
            "timestamp: %s",
            paymentId,
            userId != null ? userId : "N/A",
            "rejected",
            reason != null ? reason : "N/A",
            java.time.LocalDateTime.now()
        );
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("payment_id", paymentId);
        metadata.put("external_reference", userId);
        metadata.put("status", "rejected");
        metadata.put("reason", reason);
        sendAlert(subject, message, "WARNING", metadata);
    }
    
    /**
     * Envia alerta genérico
     * 
     * @param subject Assunto do alerta
     * @param message Mensagem do alerta
     * @param severity Severidade: CRITICAL, HIGH, MEDIUM, LOW
     */
    public void sendAlert(String subject, String message, String severity) {
        log.warn("📢 ALERTA [{}]: {} - {}", severity, subject, message);
        
        // Enviar email se habilitado
        if (emailEnabled && emailService != null) {
            try {
                emailService.sendAlertEmail(subject, message, severity);
            } catch (Exception e) {
                log.error("Erro ao enviar alerta por email: {}", e.getMessage());
            }
        }
        
        // Enviar webhook se habilitado
        if (webhookEnabled && webhookUrl != null && !webhookUrl.isEmpty()) {
            try {
                sendWebhookAlert(subject, message, severity, Collections.emptyMap());
            } catch (Exception e) {
                log.error("Erro ao enviar alerta por webhook: {}", e.getMessage());
            }
        }
    }

    public void sendAlert(String subject, String message, String severity, Map<String, Object> metadata) {
        log.warn("📢 ALERTA [{}]: {} - {}", severity, subject, message);
        if (emailEnabled && emailService != null) {
            try {
                emailService.sendAlertEmail(subject, message, severity);
            } catch (Exception e) {
                log.error("Erro ao enviar alerta por email: {}", e.getMessage());
            }
        }
        if (webhookEnabled && webhookUrl != null && !webhookUrl.isEmpty()) {
            try {
                sendWebhookAlert(subject, message, severity, metadata != null ? metadata : Collections.emptyMap());
            } catch (Exception e) {
                log.error("Erro ao enviar alerta por webhook: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Envia alerta via webhook (Slack, Teams, etc.)
     */
    private void sendWebhookAlert(String subject, String message, String severity, Map<String, Object> metadata) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("subject", subject);
            payload.put("message", message);
            payload.put("severity", severity);
            payload.put("timestamp", java.time.LocalDateTime.now().toString());
            payload.put("source", "webhook-monitoring");
            if (metadata != null && !metadata.isEmpty()) {
                payload.putAll(metadata);
            }
            
            // Usar RestTemplate ou WebClient para enviar
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String result = restTemplate.postForObject(webhookUrl, payload, String.class);
            if (result == null) {
                result = "OK";
            }
            
            log.debug("Alerta enviado via webhook para: {}", webhookUrl);
        } catch (Exception e) {
            log.error("Erro ao enviar alerta via webhook: {}", e.getMessage());
        }
    }
}
