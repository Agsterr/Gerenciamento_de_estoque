package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade que representa o histórico de alterações de cartões de pagamento
 * Mantém registro de todas as mudanças de cartão para auditoria
 */
@Entity
@Table(name = "card_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Assinatura relacionada
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;
    
    /**
     * ID do cartão anterior
     */
    @Column(name = "old_card_id", length = 255)
    private String oldCardId;
    
    /**
     * ID do novo cartão
     */
    @Column(name = "new_card_id", nullable = false, length = 255)
    private String newCardId;
    
    /**
     * Data/hora da atualização
     */
    @Column(name = "updated_at", nullable = false, updatable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Origem da atualização (webhook, admin, etc.)
     */
    @Column(name = "updated_by", length = 100)
    private String updatedBy = "webhook";
    
    /**
     * Motivo da alteração do cartão
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (updatedBy == null) {
            updatedBy = "webhook";
        }
    }
}







