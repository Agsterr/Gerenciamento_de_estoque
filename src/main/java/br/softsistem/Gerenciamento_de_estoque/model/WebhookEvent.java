package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade que representa um evento de webhook processado
 * Usada para garantir idempotência - evitar processamento duplicado de webhooks
 */
@Entity
@Table(name = "webhook_events", 
       uniqueConstraints = @UniqueConstraint(name = "uk_webhook_events_event_id", columnNames = "event_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID único do evento do webhook
     * Vem do campo "id" do payload do Mercado Pago
     * Exemplo: para payload { "type": "payment", "data": { "id": "123456789" } }
     * o eventId será "123456789"
     */
    @Column(name = "event_id", unique = true, nullable = false, length = 255)
    private String eventId;
    
    /**
     * Indica se o evento foi completamente processado
     * false = evento recebido mas ainda não processado (pode ter falhado)
     * true = evento processado com sucesso
     */
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;
    
    /**
     * Timestamp de quando o evento foi processado com sucesso
     * null = ainda não foi processado
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    /**
     * Mensagem de erro caso o processamento tenha falhado
     * null = sem erro ou ainda não processado
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    /**
     * Construtor simplificado para criar apenas com eventId
     */
    public WebhookEvent(String eventId) {
        this.eventId = eventId;
        this.createdAt = LocalDateTime.now();
    }
}

