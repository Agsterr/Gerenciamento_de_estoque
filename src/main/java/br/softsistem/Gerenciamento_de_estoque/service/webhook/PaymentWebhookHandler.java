package br.softsistem.Gerenciamento_de_estoque.service.webhook;

import br.softsistem.Gerenciamento_de_estoque.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler específico para eventos de pagamento (payment)
 * 
 * Separa a lógica de processamento de pagamentos das outras regras de negócio.
 */
@Component
public class PaymentWebhookHandler implements WebhookEventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookHandler.class);
    
    private final WebhookService webhookService;
    
    @Autowired
    public PaymentWebhookHandler(@Lazy WebhookService webhookService) {
        this.webhookService = webhookService;
    }
    
    @Override
    public void handle(String dataId, Map<String, Object> payload, Map<String, Object> notificationData) throws Exception {
        log.info("Processando evento de pagamento: {}", dataId);
        
        Map<String, Object> paymentData = (Map<String, Object>) notificationData.get("payment");
        if (paymentData != null) {
            // Delegar para o método específico de processamento de pagamento
            webhookService.handleMercadoPagoPaymentInternal(paymentData);
        } else {
            log.warn("Dados de pagamento não encontrados no payload para evento: {}", dataId);
        }
    }
    
    @Override
    public String getEventType() {
        return "payment";
    }
    
    @Override
    public boolean canHandle(String eventType) {
        return "payment".equalsIgnoreCase(eventType);
    }
}

