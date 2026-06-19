package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade que representa um evento de webhook que falhou no processamento
 * Usada para não perder eventos e permitir reprocessamento manual
 */
@Entity
@Table(name = "failed_webhook_events",
       uniqueConstraints = @UniqueConstraint(name = "idx_failed_webhook_events_event_id", columnNames = "event_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FailedWebhookEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID único do evento do webhook
     */
    @Column(name = "event_id", unique = true, nullable = false, length = 255)
    private String eventId;
    
    /**
     * Tipo do evento (payment, merchant_order, chargebacks, etc.)
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    /**
     * Payload completo do webhook (JSON)
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    /**
     * Mensagem de erro que causou a falha
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Stack trace completo do erro
     */
    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;
    
    /**
     * Número de tentativas de reprocessamento
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    /**
     * Data/hora da última tentativa de reprocessamento
     */
    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Incrementa o contador de tentativas
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount != null ? this.retryCount : 0) + 1;
        this.lastRetryAt = LocalDateTime.now();
    }
}







