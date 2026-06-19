package br.softsistem.Gerenciamento_de_estoque.enumeracao;

/**
 * Enum que representa os tipos de eventos possíveis recebidos via Webhook do Mercado Pago
 * 
 * Cada tipo de evento deve ser processado por seu handler específico,
 * garantindo separação de responsabilidades e não misturar regras de negócio.
 */
public enum MercadoPagoWebhookEventType {
    
    /**
     * Evento de pagamento
     * Processado por: PaymentWebhookHandler
     */
    PAYMENT("payment"),
    
    /**
     * Evento de pedido do comerciante
     * Processado por: MerchantOrderWebhookHandler
     */
    MERCHANT_ORDER("merchant_order"),
    
    /**
     * Evento de chargeback (estorno)
     * Processado por: ChargebackWebhookHandler
     */
    CHARGEBACKS("chargebacks"),
    
    /**
     * Evento de assinatura
     * Processado por: SubscriptionWebhookHandler
     */
    SUBSCRIPTIONS("subscriptions"),

    /**
     * Evento de assinatura (preapproval): authorized, cancelled, payment_failed, etc.
     * Processado por: SubscriptionWebhookHandler
     */
    SUBSCRIPTION_PREAPPROVAL("subscription_preapproval"),

    /**
     * Evento de pagamento autorizado para renovação de assinatura
     * Processado por: SubscriptionWebhookHandler
     */
    SUBSCRIPTION_AUTHORIZED_PAYMENT("subscription_authorized_payment"),
    
    /**
     * Evento de atualização de cartão
     * Processado por: CardWebhookHandler
     */
    CARD_UPDATED("card.updated");
    
    private final String value;
    
    MercadoPagoWebhookEventType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Converte string do webhook para enum
     * 
     * @param type String recebida no webhook (ex: "payment", "merchant_order")
     * @return Enum correspondente ou null se não encontrado
     */
    public static MercadoPagoWebhookEventType fromString(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        
        for (MercadoPagoWebhookEventType eventType : values()) {
            if (eventType.value.equalsIgnoreCase(type)) {
                return eventType;
            }
        }
        
        return null;
    }
    
    /**
     * Verifica se o tipo de evento é suportado
     */
    public static boolean isSupported(String type) {
        return fromString(type) != null;
    }
}







