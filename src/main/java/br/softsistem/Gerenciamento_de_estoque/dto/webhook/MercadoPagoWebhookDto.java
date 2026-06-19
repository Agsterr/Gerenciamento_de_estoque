package br.softsistem.Gerenciamento_de_estoque.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para receber payload genérico de webhook do Mercado Pago
 *
 * Exemplos de payload:
 *
 * {
 *   "type": "payment",
 *   "action": "payment.updated",
 *   "data": {
 *     "id": "123456"
 *   }
 * }
 *
 * {
 *   "type": "subscription_preapproval",
 *   "entity": "preapproval",
 *   "data": {
 *     "id": "987654"
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MercadoPagoWebhookDto {

    /**
     * Tipo do evento (payment, subscription_preapproval, etc)
     */
    @JsonProperty("type")
    private String type;

    /**
     * Ação do evento (ex: payment.created, payment.updated, subscription_preapproval).
     * Usado por SubscriptionWebhookHandler e ChargebackWebhookHandler.
     */
    @JsonProperty("action")
    private String action;

    /**
     * Dados do evento (sempre vem com um ID)
     */
    @JsonProperty("data")
    private WebhookData data;

    /**
     * Representa o objeto "data" do webhook
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookData {

        /**
         * ID do recurso (payment ID, preapproval ID, etc)
         */
        @JsonProperty("id")
        private String id;
    }
}


