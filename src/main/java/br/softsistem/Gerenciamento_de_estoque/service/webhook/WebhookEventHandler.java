package br.softsistem.Gerenciamento_de_estoque.service.webhook;

import java.util.Map;

/**
 * Interface para handlers de eventos de webhook do Mercado Pago
 * 
 * Strategy Pattern: cada tipo de evento tem seu handler específico,
 * garantindo separação de responsabilidades e não misturar regras de negócio.
 */
public interface WebhookEventHandler {
    
    /**
     * Processa o evento recebido do webhook
     * 
     * @param dataId ID do recurso relacionado ao evento (ex: payment_id, order_id)
     * @param payload Payload completo do webhook
     * @param notificationData Dados processados da notificação do Mercado Pago
     * @throws Exception se houver erro no processamento
     */
    void handle(String dataId, Map<String, Object> payload, Map<String, Object> notificationData) throws Exception;
    
    /**
     * Retorna o tipo de evento que este handler processa
     */
    String getEventType();
    
    /**
     * Verifica se este handler pode processar o tipo de evento especificado
     */
    boolean canHandle(String eventType);
}







