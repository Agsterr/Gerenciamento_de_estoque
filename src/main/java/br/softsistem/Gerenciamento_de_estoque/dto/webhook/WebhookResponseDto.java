package br.softsistem.Gerenciamento_de_estoque.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de processamento de webhook
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponseDto {
    
    private String status;
    private String eventId;
    private String eventType;
    private String message;
    
    public static WebhookResponseDto success(String eventId, String eventType) {
        return new WebhookResponseDto("success", eventId, eventType, "Evento processado com sucesso");
    }
    
    public static WebhookResponseDto alreadyProcessed(String eventId, String eventType) {
        return new WebhookResponseDto("already_processed", eventId, eventType, "Evento já foi processado anteriormente");
    }
    
    public static WebhookResponseDto error(String message) {
        return new WebhookResponseDto("error", null, null, message);
    }
}







