package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.FailedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operações com eventos de webhook que falharam
 */
@Repository
public interface FailedWebhookEventRepository extends JpaRepository<FailedWebhookEvent, Long> {
    
    /**
     * Busca evento falhado pelo eventId
     */
    Optional<FailedWebhookEvent> findByEventId(String eventId);
    
    /**
     * Busca eventos falhados por tipo
     */
    List<FailedWebhookEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);
    
    /**
     * Busca todos os eventos falhados ordenados por data de criação
     */
    List<FailedWebhookEvent> findAllByOrderByCreatedAtDesc();
    
    /**
     * Conta eventos falhados criados após uma data específica
     */
    long countByCreatedAtAfter(LocalDateTime afterDate);
    
    /**
     * Conta total de eventos falhados
     */
    long count();

    /**
     * Busca eventos elegíveis para reprocessamento:
     * - retryCount menor que o limite
     * - criados entre (after ... before)
     * Ordenados por data de criação ascendente
     */
    List<FailedWebhookEvent> findByRetryCountLessThanAndCreatedAtBetweenOrderByCreatedAtAsc(
            Integer retryCount,
            LocalDateTime after,
            LocalDateTime before
    );
}
