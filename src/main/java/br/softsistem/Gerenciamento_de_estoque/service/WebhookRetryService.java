package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.FailedWebhookEvent;
import br.softsistem.Gerenciamento_de_estoque.model.WebhookEvent;
import br.softsistem.Gerenciamento_de_estoque.repository.FailedWebhookEventRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.WebhookEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service para coordenação de retries de webhooks com base em FailedWebhookEvent
 * 
 * Funcionalidades:
 * - Seleciona eventos falhados elegíveis para retry
 * - Reprocessa via WebhookService usando o payload original armazenado
 * - Mantém idempotência (ignora eventos já processados)
 */
@Service
public class WebhookRetryService {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookRetryService.class);
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int MIN_AGE_MINUTES = 5;
    private static final int MAX_AGE_DAYS = 30;
    private static final int BATCH_LIMIT = 50;
    
    private final FailedWebhookEventRepository failedWebhookEventRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;
    
    /**
     * Construtor com injeções necessárias
     */
    @Autowired
    public WebhookRetryService(
            FailedWebhookEventRepository failedWebhookEventRepository,
            WebhookEventRepository webhookEventRepository,
            WebhookService webhookService) {
        this.failedWebhookEventRepository = failedWebhookEventRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.webhookService = webhookService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Reprocessa em lote eventos falhados elegíveis
     */
    @Transactional
    public void reprocessEligibleFailedEventsBatch() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime minAge = now.minusMinutes(MIN_AGE_MINUTES);
            LocalDateTime maxAge = now.minusDays(MAX_AGE_DAYS);
            List<FailedWebhookEvent> failedEvents = failedWebhookEventRepository
                    .findByRetryCountLessThanAndCreatedAtBetweenOrderByCreatedAtAsc(
                            MAX_RETRY_ATTEMPTS, maxAge, minAge
                    );
            if (failedEvents.size() > BATCH_LIMIT) {
                failedEvents = failedEvents.subList(0, BATCH_LIMIT);
            }
            if (failedEvents.isEmpty()) {
                log.info("Nenhum FailedWebhookEvent elegível para retry");
                return;
            }
            log.info("Encontrados {} FailedWebhookEvent(s) para retry", failedEvents.size());
            int successCount = 0;
            int failureCount = 0;
            int skippedCount = 0;
            for (FailedWebhookEvent failedEvent : failedEvents) {
                try {
                    Optional<WebhookEvent> eventOpt = webhookEventRepository.findByEventId(failedEvent.getEventId());
                    if (eventOpt.isPresent() && Boolean.TRUE.equals(eventOpt.get().getProcessed())) {
                        skippedCount++;
                        continue;
                    }
                    if (reprocessSingleFailedEvent(failedEvent)) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.warn("Erro ao reprocessar evento {}: {}", failedEvent.getEventId(), e.getMessage());
                    failedEvent.incrementRetryCount();
                    failedEvent.setErrorMessage("Erro no retry: " + e.getMessage());
                    failedWebhookEventRepository.save(failedEvent);
                    failureCount++;
                }
            }
            log.info("Retry concluído: sucessos={}, falhas={}, ignorados={}", successCount, failureCount, skippedCount);
        } catch (Exception e) {
            log.error("Erro crítico no retry de webhooks: {}", e.getMessage());
        }
    }
    
    /**
     * Reprocessa um evento falhado específico usando o payload original armazenado
     * 
     * @param eventId ID do evento a ser reprocessado
     * @return true se reprocessado com sucesso, false caso contrário
     */
    @Transactional
    public boolean reprocessEvent(String eventId) {
        try {
            Optional<FailedWebhookEvent> failedOpt = failedWebhookEventRepository.findByEventId(eventId);
            if (failedOpt.isEmpty()) {
                log.warn("FailedWebhookEvent {} não encontrado para reprocessamento", eventId);
                return false;
            }
            
            FailedWebhookEvent failedEvent = failedOpt.get();
            Optional<WebhookEvent> eventOpt = webhookEventRepository.findByEventId(eventId);
            WebhookEvent event = eventOpt.orElse(null);
            
            if (event != null && Boolean.TRUE.equals(event.getProcessed())) {
                log.info("Evento {} já foi processado, ignorando (idempotência)", eventId);
                return false;
            }
            
            log.info("Reprocessando evento: eventId={}", eventId);
            
            String requestBody = failedEvent.getPayload();
            Map<String, Object> payload = objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {});
            
            webhookService.handleMercadoPagoWebhook(payload, "reprocess-signature", "reprocess-" + eventId, requestBody);
            
            if (event != null) {
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                event.setErrorMessage(null);
                webhookEventRepository.save(event);
            }
            failedWebhookEventRepository.delete(failedEvent);
            
            log.info("✓ Evento {} reprocessado com sucesso", eventId);
            return true;
            
        } catch (Exception e) {
            log.warn("Erro ao reprocessar evento {}: {}", eventId, e.getMessage());
            failedWebhookEventRepository.findByEventId(eventId).ifPresent(failed -> {
                failed.incrementRetryCount();
                failed.setErrorMessage("Erro no reprocessamento: " + e.getMessage());
                failedWebhookEventRepository.save(failed);
            });
            return false;
        }
    }
    
    private boolean reprocessSingleFailedEvent(FailedWebhookEvent failedEvent) {
        try {
            Optional<WebhookEvent> eventOpt = webhookEventRepository.findByEventId(failedEvent.getEventId());
            WebhookEvent event = eventOpt.orElse(null);
            if (event != null && Boolean.TRUE.equals(event.getProcessed())) {
                return false;
            }
            Map<String, Object> payload = objectMapper.readValue(failedEvent.getPayload(), new TypeReference<Map<String, Object>>() {});
            webhookService.handleMercadoPagoWebhook(payload, "reprocess-signature", "reprocess-" + failedEvent.getEventId(), failedEvent.getPayload());
            if (event != null) {
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                event.setErrorMessage(null);
                webhookEventRepository.save(event);
            }
            failedWebhookEventRepository.delete(failedEvent);
            return true;
        } catch (Exception e) {
            failedEvent.incrementRetryCount();
            failedEvent.setErrorMessage("Erro no reprocessamento: " + e.getMessage());
            failedWebhookEventRepository.save(failedEvent);
            return false;
        }
    }
}
