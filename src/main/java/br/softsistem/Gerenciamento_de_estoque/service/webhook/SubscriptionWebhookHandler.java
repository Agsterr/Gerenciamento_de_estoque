package br.softsistem.Gerenciamento_de_estoque.service.webhook;

import br.softsistem.Gerenciamento_de_estoque.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler específico para eventos de assinatura (subscriptions)
 * 
 * Trata eventos:
 * - subscription_preapproval: Criação/atualização de assinatura
 * - subscription_authorized_payment: Pagamento autorizado para renovação mensal
 * 
 * Separa a lógica de processamento de assinaturas das regras de pagamento.
 */
@Component
public class SubscriptionWebhookHandler implements WebhookEventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(SubscriptionWebhookHandler.class);
    
    private final WebhookService webhookService;
    
    @Autowired
    public SubscriptionWebhookHandler(@Lazy WebhookService webhookService) {
        this.webhookService = webhookService;
    }
    
    @Override
    public void handle(String dataId, Map<String, Object> payload, Map<String, Object> notificationData) throws Exception {
        log.info("=== Processando evento de assinatura ===");
        log.info("Subscription ID: {}", dataId);
        
        // Verificar tipo de evento de assinatura
        String action = (String) payload.get("action");
        String type = (String) payload.get("type");
        
        log.info("Tipo de evento: {}, Action: {}", type, action);
        
        // Tratar subscription_preapproval
        if ("subscription_preapproval".equalsIgnoreCase(type) || "preapproval".equalsIgnoreCase(type)) {
            Map<String, Object> preapprovalData = (Map<String, Object>) notificationData.get("preapproval");
            if (preapprovalData == null) {
                preapprovalData = (Map<String, Object>) notificationData.get("subscription");
            }
            
            if (preapprovalData != null) {
                log.info("Processando subscription_preapproval");
                webhookService.handleSubscriptionPreapproval(preapprovalData, action);
            } else {
                log.warn("Dados de preapproval não encontrados no payload para evento: {}", dataId);
            }
        }
        // Tratar subscription_authorized_payment (renovação mensal)
        else if ("subscription_authorized_payment".equalsIgnoreCase(type)) {
            Map<String, Object> paymentData = (Map<String, Object>) notificationData.get("payment");
            if (paymentData != null) {
                log.info("Processando subscription_authorized_payment (renovação mensal)");
                webhookService.handleSubscriptionAuthorizedPayment(dataId, paymentData);
            } else {
                log.warn("Dados de pagamento autorizado não encontrados no payload para evento: {}", dataId);
            }
        }
        // Fallback para formato antigo
        else {
            Map<String, Object> subscriptionData = (Map<String, Object>) notificationData.get("preapproval");
            if (subscriptionData != null) {
                log.info("Processando assinatura (formato antigo)");
                webhookService.handleMercadoPagoPreApprovalInternal(subscriptionData);
            } else {
                log.warn("Dados de assinatura não encontrados no payload para evento: {}", dataId);
            }
        }
        
        log.info("=== Processamento de assinatura concluído ===");
    }
    
    @Override
    public String getEventType() {
        return "subscriptions";
    }
    
    @Override
    public boolean canHandle(String eventType) {
        return "subscriptions".equalsIgnoreCase(eventType) || 
               "preapproval".equalsIgnoreCase(eventType) ||
               "subscription_preapproval".equalsIgnoreCase(eventType) ||
               "subscription_authorized_payment".equalsIgnoreCase(eventType);
    }
}

