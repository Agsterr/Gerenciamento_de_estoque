package br.softsistem.Gerenciamento_de_estoque.exception;

/**
 * Exceção específica para erros durante o processamento de webhooks
 */
public class WebhookProcessingException extends RuntimeException {
    
    private final String eventId;
    private final String eventType;
    
    public WebhookProcessingException(String message) {
        super(message);
        this.eventId = null;
        this.eventType = null;
    }
    
    public WebhookProcessingException(String message, String eventId, String eventType) {
        super(message);
        this.eventId = eventId;
        this.eventType = eventType;
    }
    
    public WebhookProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.eventId = null;
        this.eventType = null;
    }
    
    public WebhookProcessingException(String message, String eventId, String eventType, Throwable cause) {
        super(message, cause);
        this.eventId = eventId;
        this.eventType = eventType;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
}