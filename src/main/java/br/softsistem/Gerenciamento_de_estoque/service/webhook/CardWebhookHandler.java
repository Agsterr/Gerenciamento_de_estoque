package br.softsistem.Gerenciamento_de_estoque.service.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handler específico para eventos de atualização de cartão (card.updated)
 *
 * IMPORTANTE: O card_id NÃO deve ser salvo no banco. O Mercado Pago gerencia o cartão
 * internamente na assinatura. Persistir card_id causa problemas na aquisição do MP.
 *
 * Este handler apenas registra o evento em log para auditoria, sem persistir dados de cartão.
 */
@Component
public class CardWebhookHandler implements WebhookEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CardWebhookHandler.class);

    @Override
    @Transactional
    public void handle(String dataId, Map<String, Object> payload, Map<String, Object> notificationData)
            throws Exception {
        log.info("=== Processando evento de atualização de cartão (card.updated) ===");
        log.info("Card ID: {}", dataId);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cardData = (Map<String, Object>) notificationData.get("card");
            if (cardData == null) {
                log.warn("Dados de cartão não encontrados no payload para evento: {}", dataId);
                return;
            }

            // Extrair informações do cartão
            String cardId = extractCardId(cardData, dataId);
            String customerId = extractCustomerId(cardData);

            if (cardId == null || cardId.isEmpty()) {
                log.error("Card ID não encontrado no payload para evento: {}", dataId);
                return;
            }

            log.info("Card ID: {}, Customer ID: {}", cardId, customerId);

            // NÃO salvar card_id no banco - causa problemas na aquisição do Mercado Pago.
            // O MP gerencia o cartão internamente; external_reference (user ID) é o identificador único.
            logCardUpdate(null, cardId, "Evento recebido - card_id NÃO persistido (política de segurança)");

            log.info("=== Processamento de card.updated concluído (apenas auditoria em log) ===");

        } catch (Exception e) {
            log.error("Erro ao processar evento card.updated: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Extrai o card_id dos dados do cartão
     */
    private String extractCardId(Map<String, Object> cardData, String fallbackId) {
        // Tentar diferentes campos possíveis
        Object idObj = cardData.get("id");
        if (idObj != null) {
            return idObj.toString();
        }

        idObj = cardData.get("card_id");
        if (idObj != null) {
            return idObj.toString();
        }

        // Usar fallback se não encontrar
        return fallbackId;
    }

    /**
     * Extrai o customer_id dos dados do cartão
     */
    private String extractCustomerId(Map<String, Object> cardData) {
        Object customerIdObj = cardData.get("customer_id");
        if (customerIdObj != null) {
            return customerIdObj.toString();
        }

        // Tentar outros campos possíveis
        customerIdObj = cardData.get("payer_id");
        if (customerIdObj != null) {
            return customerIdObj.toString();
        }

        customerIdObj = cardData.get("user_id");
        if (customerIdObj != null) {
            return customerIdObj.toString();
        }

        return null;
    }

    /**
     * Loga alterações de cartão para auditoria (sem persistir dados)
     */
    private void logCardUpdate(String oldCardId, String newCardId, String reason) {
        log.info("=== AUDITORIA: Atualização de Cartão (não persistido) ===");
        log.info("Data/Hora: {}", LocalDateTime.now());
        log.info("Card ID Anterior: {}", oldCardId != null ? oldCardId : "null");
        log.info("Card ID Novo: {}", newCardId);
        log.info("Motivo: {}", reason);
        log.info("Origem: Webhook card.updated");
        log.info("========================================");
    }

    @Override
    public String getEventType() {
        return "card.updated";
    }

    @Override
    public boolean canHandle(String eventType) {
        return "card.updated".equalsIgnoreCase(eventType);
    }
}
