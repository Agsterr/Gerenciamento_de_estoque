package br.softsistem.Gerenciamento_de_estoque.service.webhook;

import br.softsistem.Gerenciamento_de_estoque.model.ChargebackHistory;
import br.softsistem.Gerenciamento_de_estoque.model.Payment;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.repository.ChargebackHistoryRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.PaymentRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import br.softsistem.Gerenciamento_de_estoque.service.WebhookMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Handler específico para eventos de chargeback (estorno) e reclamações
 * 
 * Evento: chargebacks
 * 
 * Requisitos:
 * - Identificar criação ou mudança de status
 * - Marcar pagamento como em disputa
 * - Bloquear entrega ou acesso ao serviço
 * - Registrar histórico do chargeback
 * - NÃO permitir reprocessamento automático
 */
@Component
public class ChargebackWebhookHandler implements WebhookEventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ChargebackWebhookHandler.class);
    
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ChargebackHistoryRepository chargebackHistoryRepository;
    private final WebhookMonitoringService monitoringService;
    
    @Autowired
    public ChargebackWebhookHandler(PaymentRepository paymentRepository, 
                                    SubscriptionRepository subscriptionRepository,
                                    ChargebackHistoryRepository chargebackHistoryRepository,
                                    WebhookMonitoringService monitoringService) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.chargebackHistoryRepository = chargebackHistoryRepository;
        this.monitoringService = monitoringService;
    }
    
    @Override
    @Transactional
    public void handle(String dataId, Map<String, Object> payload, Map<String, Object> notificationData) throws Exception {
        log.error("=== PROCESSANDO EVENTO DE CHARGEBACK/RECLAMAÇÃO ===");
        log.error("Chargeback ID: {}", dataId);
        
        // 1. Identificar criação ou mudança de status
        String action = extractAction(payload);
        String status = extractStatus(notificationData, dataId);
        
        log.error("Action: {}, Status: {}", action, status);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> chargebackData = (Map<String, Object>) notificationData.get("chargeback");
        if (chargebackData == null) {
            log.error("✗ Dados de chargeback não encontrados no payload para evento: {}", dataId);
            throw new RuntimeException("Dados de chargeback não encontrados");
        }
        
        // Extrair payment_id do chargeback
        String paymentIdStr = extractPaymentIdFromChargeback(chargebackData);
        if (paymentIdStr == null || paymentIdStr.isEmpty()) {
            log.error("✗ Payment ID não encontrado no chargeback: {}", chargebackData);
            throw new RuntimeException("Payment ID não encontrado no chargeback");
        }
        
        Long paymentId;
        try {
            paymentId = Long.parseLong(paymentIdStr);
        } catch (NumberFormatException e) {
            log.error("✗ Payment ID inválido no chargeback: {}", paymentIdStr);
            throw new RuntimeException("Payment ID inválido: " + paymentIdStr);
        }
        
        log.error("Payment ID do chargeback: {}", paymentId);
        
        // Buscar payment
        Optional<Payment> paymentOpt = paymentRepository.findByMercadoPagoPaymentId(paymentId);
        if (paymentOpt.isEmpty()) {
            log.error("✗ Payment não encontrado para chargeback: {}", paymentId);
            throw new RuntimeException("Payment não encontrado: " + paymentId);
        }
        
        Payment payment = paymentOpt.get();
        Subscription subscription = payment.getSubscription();
        
        // Verificar se já processamos este status para evitar reprocessamento
        if (chargebackHistoryRepository.existsByChargebackIdAndPaymentIdAndStatus(dataId, payment.getId(), status)) {
            log.warn("⚠ Chargeback {} com status {} já foi processado para payment {}. Ignorando.", 
                dataId, status, payment.getId());
            return;
        }
        
        // 2. Marcar pagamento como em disputa
        payment.setStatus(Payment.PaymentStatus.CHARGED_BACK);
        payment.setInDispute(true);
        payment.setFailedAt(LocalDateTime.now());
        payment.setFailureReason("Chargeback detectado via webhook - Status: " + status + " - Acesso bloqueado");
        paymentRepository.save(payment);
        log.error("✓ Payment marcado como em disputa (CHARGED_BACK): {}", payment.getId());
        
        // 3. Bloquear entrega ou acesso ao serviço
        if (subscription != null) {
            // Suspender assinatura
            SubscriptionStatus previousStatus = subscription.getStatus();
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
            subscription.setAccessBlocked(true); // Bloquear acesso ao serviço
            subscription.setEndedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            log.error("✓ Subscription suspensa e acesso bloqueado: {}", subscription.getId());
            log.error("  Status anterior: {} → Status atual: PAST_DUE", previousStatus);
            log.error("  Access Blocked: true");
        } else {
            log.error("✗ Subscription não encontrada para payment: {}", paymentId);
        }
        
        // 4. Registrar histórico do chargeback
        ChargebackHistory history = new ChargebackHistory();
        history.setChargebackId(dataId);
        history.setPayment(payment);
        history.setSubscription(subscription);
        if (subscription != null && subscription.getUser() != null) {
            history.setUserId(subscription.getUser().getId());
        }
        history.setStatus(status);
        history.setAction(action != null ? action : "chargeback.unknown");
        history.setReason(extractReason(chargebackData));
        history.setAmount(extractAmount(chargebackData, payment));
        chargebackHistoryRepository.save(history);
        log.error("✓ Histórico de chargeback registrado: ID={}, Status={}, Action={}", 
            history.getId(), status, action);
        
        // 5. NÃO permitir reprocessamento automático
        // A flag access_blocked garante que mesmo que a assinatura seja reativada,
        // o acesso permanece bloqueado até intervenção manual
        log.error("=== HISTÓRICO DE CHARGEBACK ===");
        log.error("Data/Hora: {}", LocalDateTime.now());
        log.error("Chargeback ID: {}", dataId);
        log.error("Payment ID: {}", paymentId);
        log.error("Payment Local ID: {}", payment.getId());
        if (subscription != null) {
            log.error("Subscription ID: {}", subscription.getId());
            log.error("User ID: {}", subscription.getUser() != null ? subscription.getUser().getId() : "null");
        }
        log.error("Status: {}", status);
        log.error("Action: {}", action);
        log.error("Ação executada: Payment em disputa, Subscription suspensa, Acesso bloqueado");
        log.error("Reativação automática: NÃO PERMITIDA");
        log.error("Desbloqueio de acesso: Requer intervenção manual");
        log.error("=================================");
        
        // 🚨 ALERTA IMEDIATO DE CHARGEBACK
        String subscriptionIdStr = subscription != null ? String.valueOf(subscription.getId()) : null;
        monitoringService.recordChargebackDetected(String.valueOf(paymentId), subscriptionIdStr);
        
        log.error("=== PROCESSAMENTO DE CHARGEBACK CONCLUÍDO ===");
    }
    
    /**
     * Extrai a ação do payload (chargeback.created, chargeback.updated, etc.)
     */
    private String extractAction(Map<String, Object> payload) {
        Object actionObj = payload.get("action");
        if (actionObj != null) {
            return actionObj.toString();
        }
        return "chargeback.unknown";
    }
    
    /**
     * Extrai o status do chargeback
     */
    private String extractStatus(Map<String, Object> notificationData, String fallbackId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> chargebackData = (Map<String, Object>) notificationData.get("chargeback");
        if (chargebackData != null) {
            Object statusObj = chargebackData.get("status");
            if (statusObj != null) {
                return statusObj.toString();
            }
        }
        // Se não encontrar, usar "created" como padrão para novos chargebacks
        return "created";
    }
    
    /**
     * Extrai o motivo do chargeback
     */
    private String extractReason(Map<String, Object> chargebackData) {
        Object reasonObj = chargebackData.get("reason");
        if (reasonObj != null) {
            return reasonObj.toString();
        }
        
        reasonObj = chargebackData.get("dispute_reason");
        if (reasonObj != null) {
            return reasonObj.toString();
        }
        
        return "Chargeback detectado via webhook";
    }
    
    /**
     * Extrai o valor do chargeback
     */
    private BigDecimal extractAmount(Map<String, Object> chargebackData, Payment payment) {
        Object amountObj = chargebackData.get("amount");
        if (amountObj != null) {
            try {
                if (amountObj instanceof Number) {
                    return BigDecimal.valueOf(((Number) amountObj).doubleValue());
                } else if (amountObj instanceof String) {
                    return new BigDecimal((String) amountObj);
                }
            } catch (Exception e) {
                log.debug("Erro ao extrair amount do chargeback: {}", e.getMessage());
            }
        }
        // Fallback: usar valor do payment
        return payment != null ? payment.getAmount() : null;
    }
    
    /**
     * Extrai payment_id do objeto chargeback
     */
    @SuppressWarnings("unchecked")
    private String extractPaymentIdFromChargeback(Map<String, Object> chargebackData) {
        // Tentar diferentes campos possíveis
        Object paymentIdObj = chargebackData.get("payment_id");
        if (paymentIdObj == null) {
            paymentIdObj = chargebackData.get("paymentId");
        }
        if (paymentIdObj == null) {
            paymentIdObj = chargebackData.get("payment");
        }
        if (paymentIdObj == null && chargebackData.get("payment") instanceof Map) {
            Map<String, Object> paymentMap = (Map<String, Object>) chargebackData.get("payment");
            paymentIdObj = paymentMap.get("id");
        }
        
        return paymentIdObj != null ? paymentIdObj.toString() : null;
    }
    
    @Override
    public String getEventType() {
        return "chargebacks";
    }
    
    @Override
    public boolean canHandle(String eventType) {
        return "chargebacks".equalsIgnoreCase(eventType) || 
               "topic_chargebacks_wh".equalsIgnoreCase(eventType);
    }
}

