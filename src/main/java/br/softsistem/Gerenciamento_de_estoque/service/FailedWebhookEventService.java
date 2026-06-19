package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.FailedWebhookEvent;
import br.softsistem.Gerenciamento_de_estoque.repository.FailedWebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service para gerenciar eventos de webhook que falharam no processamento
 */
@Service
public class FailedWebhookEventService {
    
    private static final Logger log = LoggerFactory.getLogger(FailedWebhookEventService.class);
    
    private final FailedWebhookEventRepository failedWebhookEventRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public FailedWebhookEventService(FailedWebhookEventRepository failedWebhookEventRepository) {
        this.failedWebhookEventRepository = failedWebhookEventRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Salva um evento que falhou no processamento
     * 
     * @param eventId ID único do evento
     * @param eventType Tipo do evento (payment, merchant_order, etc.)
     * @param payload Payload completo do webhook (JSON)
     * @param error Exceção que causou a falha
     */
    @Transactional
    public void saveFailedEvent(String eventId, String eventType, String payload, Exception error) {
        try {
            // Verificar se já existe evento falhado com o mesmo eventId
            Optional<FailedWebhookEvent> existingOpt = failedWebhookEventRepository.findByEventId(eventId);
            
            FailedWebhookEvent failedEvent;
            if (existingOpt.isPresent()) {
                failedEvent = existingOpt.get();
                failedEvent.incrementRetryCount();
                log.info("Evento falhado já existe, incrementando retry_count: {}", eventId);
            } else {
                failedEvent = new FailedWebhookEvent();
                failedEvent.setEventId(eventId);
                failedEvent.setEventType(eventType);
            }
            
            // Atualizar dados
            failedEvent.setPayload(payload);
            failedEvent.setErrorMessage(error != null ? error.getMessage() : "Erro desconhecido");
            failedEvent.setErrorStackTrace(getStackTrace(error));
            
            failedWebhookEventRepository.save(failedEvent);
            log.error("Evento falhado salvo: eventId={}, eventType={}, retryCount={}", 
                eventId, eventType, failedEvent.getRetryCount());
            
        } catch (Exception e) {
            log.error("Erro ao salvar evento falhado: {}", e.getMessage(), e);
            // Não lançar exceção para não interromper o fluxo principal
        }
    }
    
    /**
     * Salva um evento que falhou usando Map como payload
     */
    @Transactional
    public void saveFailedEvent(String eventId, String eventType, Map<String, Object> payload, Exception error) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            saveFailedEvent(eventId, eventType, payloadJson, error);
        } catch (Exception e) {
            log.error("Erro ao converter payload para JSON: {}", e.getMessage(), e);
            // Salvar com payload vazio se não conseguir converter
            saveFailedEvent(eventId, eventType, "{}", error);
        }
    }
    
    /**
     * Busca todos os eventos falhados
     */
    public List<FailedWebhookEvent> getAllFailedEvents() {
        return failedWebhookEventRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * Busca eventos falhados por tipo
     */
    public List<FailedWebhookEvent> getFailedEventsByType(String eventType) {
        return failedWebhookEventRepository.findByEventTypeOrderByCreatedAtDesc(eventType);
    }
    
    /**
     * Busca um evento falhado pelo eventId
     */
    public Optional<FailedWebhookEvent> getFailedEventByEventId(String eventId) {
        return failedWebhookEventRepository.findByEventId(eventId);
    }
    
    /**
     * Reprocessa um evento falhado
     * 
     * @param failedEvent Evento falhado a ser reprocessado
     * @param webhookService Service para processar o webhook
     */
    @Transactional
    public void reprocessFailedEvent(FailedWebhookEvent failedEvent, WebhookService webhookService) {
        try {
            log.info("Reprocessando evento falhado: eventId={}, eventType={}, retryCount={}", 
                failedEvent.getEventId(), failedEvent.getEventType(), failedEvent.getRetryCount());
            
            // Parse do payload
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(failedEvent.getPayload(), Map.class);
            
            // Incrementar retry count antes de reprocessar
            failedEvent.incrementRetryCount();
            failedWebhookEventRepository.save(failedEvent);
            
            // Reprocessar via WebhookService
            // Criar um requestBody simulado (sem assinatura, pois já foi validado antes)
            String requestBody = failedEvent.getPayload();
            String signature = "reprocess"; // Placeholder para reprocessamento
            String requestId = failedEvent.getEventId();
            
            // Chamar processamento do webhook
            webhookService.handleMercadoPagoWebhook(payload, signature, requestId, requestBody);
            
            log.info("✓ Evento reprocessado com sucesso: {}", failedEvent.getEventId());
            
        } catch (Exception e) {
            log.error("Erro ao reprocessar evento falhado {}: {}", failedEvent.getEventId(), e.getMessage(), e);
            // Atualizar erro
            failedEvent.setErrorMessage("Erro ao reprocessar: " + e.getMessage());
            failedEvent.setErrorStackTrace(getStackTrace(e));
            failedWebhookEventRepository.save(failedEvent);
            throw new RuntimeException("Erro ao reprocessar evento: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converte stack trace para string
     */
    private String getStackTrace(Exception error) {
        if (error == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }
}

