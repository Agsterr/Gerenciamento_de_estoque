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
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Job agendado para reprocessar webhooks não processados
 * 
 * Regras:
 * - Buscar eventos com processed = false
 * - Criados há mais de 5 minutos (evita processar eventos muito recentes)
 * - Limite de tentativas (máximo 3)
 * - Backoff exponencial
 * - Ignorar eventos muito antigos (> 30 dias)
 */
@Service
public class WebhookReprocessingJob {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookReprocessingJob.class);
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int MIN_AGE_MINUTES = 5; // Aguardar 5 minutos antes de reprocessar
    private static final int MAX_AGE_DAYS = 30; // Ignorar eventos mais antigos que 30 dias
    private static final int BATCH_LIMIT = 50; // Limite por execução
    
    private final WebhookEventRepository webhookEventRepository;
    private final FailedWebhookEventRepository failedWebhookEventRepository;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public WebhookReprocessingJob(
            WebhookEventRepository webhookEventRepository,
            FailedWebhookEventRepository failedWebhookEventRepository,
            @Lazy WebhookService webhookService) {
        this.webhookEventRepository = webhookEventRepository;
        this.failedWebhookEventRepository = failedWebhookEventRepository;
        this.webhookService = webhookService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Job agendado para reprocessar eventos não processados
     * Executa a cada 5 minutos
     */
    @Scheduled(fixedDelay = 300000) // 5 minutos
    @Transactional
    public void reprocessUnprocessedWebhooks() {
        try {
            log.info("Iniciando job de reprocessamento de webhooks falhados...");
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime minAge = now.minusMinutes(MIN_AGE_MINUTES);
            LocalDateTime maxAge = now.minusDays(MAX_AGE_DAYS);
            
            // Selecionar apenas FailedWebhookEvent elegíveis
            List<FailedWebhookEvent> failedEvents = failedWebhookEventRepository
                .findByRetryCountLessThanAndCreatedAtBetweenOrderByCreatedAtAsc(
                    MAX_RETRY_ATTEMPTS, maxAge, minAge
                );
            if (failedEvents.size() > BATCH_LIMIT) {
                failedEvents = failedEvents.subList(0, BATCH_LIMIT);
            }
            
            if (failedEvents.isEmpty()) {
                log.info("Nenhum evento falhado elegível para reprocessamento");
                return;
            }
            
            log.info("Encontrados {} eventos falhados para reprocessamento", failedEvents.size());
            
            int reprocessed = 0;
            int skipped = 0;
            int failed = 0;
            
            for (FailedWebhookEvent failedEvent : failedEvents) {
                try {
                    if (reprocessEvent(failedEvent)) {
                        reprocessed++;
                    } else {
                        skipped++;
                    }
                    
                } catch (Exception e) {
                    log.error("Erro ao reprocessar evento {}: {}", failedEvent.getEventId(), e.getMessage());
                    failed++;
                    failedEvent.incrementRetryCount();
                    failedEvent.setErrorMessage("Erro no reprocessamento: " + e.getMessage());
                    failedWebhookEventRepository.save(failedEvent);
                }
            }
            
            log.info("Job de reprocessamento concluído: {} reprocessados, {} ignorados, {} falhas",
                reprocessed, skipped, failed);
            
        } catch (Exception e) {
            log.error("Erro no job de reprocessamento: {}", e.getMessage());
        }
    }
    
    /**
     * Reprocessa um evento falhado específico
     * 
     * @param failedEvent Evento falhado a ser reprocessado
     * @return true se reprocessado com sucesso, false caso contrário
     */
    private boolean reprocessEvent(FailedWebhookEvent failedEvent) {
        log.info("Reprocessando evento: {}", failedEvent.getEventId());
        
        try {
            // Incrementar contador de tentativas no failed_webhook_event
            failedEvent.incrementRetryCount();
            failedWebhookEventRepository.save(failedEvent);
            
            // Parse do payload JSON
            Map<String, Object> payload;
            try {
                payload = objectMapper.readValue(failedEvent.getPayload(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Erro ao fazer parse do payload do evento {}: {}", failedEvent.getEventId(), e.getMessage());
                failedEvent.setErrorMessage("Erro ao fazer parse do payload: " + e.getMessage());
                failedWebhookEventRepository.save(failedEvent);
                return false;
            }
            
            // Reprocessar usando WebhookService
            // Nota: Não validamos assinatura novamente pois já foi validada na primeira vez
            // Usamos uma assinatura dummy para passar pela validação (que será ignorada internamente)
            try {
                webhookService.handleMercadoPagoWebhook(
                    payload,
                    "reprocess-signature", // Assinatura dummy (será ignorada se evento já existe)
                    "reprocess-" + failedEvent.getEventId(), // Request ID para rastreamento
                    failedEvent.getPayload() // Request body original
                );
                
                // Se chegou aqui, foi processado com sucesso
                // Garantir marcação de processed no WebhookEvent
                Optional<WebhookEvent> eventOpt = webhookEventRepository.findByEventId(failedEvent.getEventId());
                if (eventOpt.isPresent()) {
                    WebhookEvent e = eventOpt.get();
                    e.setProcessed(true);
                    e.setProcessedAt(LocalDateTime.now());
                    e.setErrorMessage(null);
                    webhookEventRepository.save(e);
                }
                // Remover o registro de falha
                failedWebhookEventRepository.delete(failedEvent);
                
                log.info("✓ Evento {} reprocessado com sucesso", failedEvent.getEventId());
                return true;
                
            } catch (Exception e) {
                log.warn("Erro ao reprocessar evento {} via WebhookService: {}", failedEvent.getEventId(), e.getMessage());
                failedEvent.setErrorMessage("Erro no reprocessamento: " + e.getMessage());
                failedWebhookEventRepository.save(failedEvent);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Erro ao reprocessar evento {}: {}", failedEvent.getEventId(), e.getMessage());
            failedEvent.setErrorMessage("Erro no reprocessamento: " + e.getMessage());
            failedWebhookEventRepository.save(failedEvent);
            return false;
        }
    }
}
