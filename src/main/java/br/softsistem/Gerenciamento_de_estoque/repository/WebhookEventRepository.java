package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operações com eventos de webhook
 * Usado para garantir idempotência persistente
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    
    /**
     * Verifica se um evento com o eventId já foi processado
     * 
     * @param eventId ID único do evento do webhook
     * @return true se o evento já existe, false caso contrário
     */
    boolean existsByEventId(String eventId);
    
    /**
     * Busca um evento pelo eventId
     * 
     * @param eventId ID único do evento do webhook
     * @return Optional com o evento encontrado
     */
    Optional<WebhookEvent> findByEventId(String eventId);
    
    /**
     * Busca eventos não processados criados antes de uma data específica
     * Usado pelo job de retry automático para reprocessar eventos que falharam
     * 
     * @param beforeDate Data limite - busca eventos criados antes desta data
     * @return Lista de eventos não processados
     */
    @Query("SELECT e FROM WebhookEvent e WHERE e.processed = false AND e.createdAt < :beforeDate")
    List<WebhookEvent> findByProcessedFalseAndCreatedAtBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Busca eventos não processados (para retry)
     * 
     * @return Lista de eventos não processados
     */
    List<WebhookEvent> findByProcessedFalse();
    
    /**
     * Conta eventos não processados
     */
    long countByProcessedFalse();
    
    /**
     * Conta eventos não processados criados antes de uma data específica
     */
    @Query("SELECT COUNT(e) FROM WebhookEvent e WHERE e.processed = false AND e.createdAt < :beforeDate")
    long countByProcessedFalseAndCreatedAtBefore(@Param("beforeDate") LocalDateTime beforeDate);
}

