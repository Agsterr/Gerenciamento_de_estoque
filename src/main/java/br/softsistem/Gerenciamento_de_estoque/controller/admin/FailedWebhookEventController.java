package br.softsistem.Gerenciamento_de_estoque.controller.admin;

import br.softsistem.Gerenciamento_de_estoque.model.FailedWebhookEvent;
import br.softsistem.Gerenciamento_de_estoque.service.FailedWebhookEventService;
import br.softsistem.Gerenciamento_de_estoque.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller administrativo para gerenciar eventos de webhook que falharam
 * 
 * Endpoints para:
 * - Listar eventos falhados
 * - Reprocessar eventos falhados
 * - Visualizar detalhes de eventos falhados
 */
@RestController
@RequestMapping("/admin/webhooks/failed")
@Tag(name = "Admin - Failed Webhook Events", description = "Gerenciamento de eventos de webhook que falharam")
public class FailedWebhookEventController {
    
    private static final Logger log = LoggerFactory.getLogger(FailedWebhookEventController.class);
    
    private final FailedWebhookEventService failedWebhookEventService;
    private final WebhookService webhookService;
    
    @Autowired
    public FailedWebhookEventController(FailedWebhookEventService failedWebhookEventService,
                                       WebhookService webhookService) {
        this.failedWebhookEventService = failedWebhookEventService;
        this.webhookService = webhookService;
    }
    
    /**
     * Lista todos os eventos falhados
     */
    @GetMapping
    @Operation(summary = "Listar eventos falhados", description = "Retorna todos os eventos de webhook que falharam no processamento")
    @ApiResponse(responseCode = "200", description = "Lista de eventos falhados")
    public ResponseEntity<List<FailedWebhookEvent>> getAllFailedEvents() {
        List<FailedWebhookEvent> events = failedWebhookEventService.getAllFailedEvents();
        return ResponseEntity.ok(events);
    }
    
    /**
     * Lista eventos falhados por tipo
     */
    @GetMapping("/type/{eventType}")
    @Operation(summary = "Listar eventos falhados por tipo", description = "Retorna eventos falhados de um tipo específico")
    @ApiResponse(responseCode = "200", description = "Lista de eventos falhados do tipo especificado")
    public ResponseEntity<List<FailedWebhookEvent>> getFailedEventsByType(@PathVariable String eventType) {
        List<FailedWebhookEvent> events = failedWebhookEventService.getFailedEventsByType(eventType);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Busca um evento falhado pelo eventId
     */
    @GetMapping("/{eventId}")
    @Operation(summary = "Buscar evento falhado", description = "Retorna detalhes de um evento falhado específico")
    @ApiResponse(responseCode = "200", description = "Evento falhado encontrado")
    @ApiResponse(responseCode = "404", description = "Evento falhado não encontrado")
    public ResponseEntity<FailedWebhookEvent> getFailedEvent(@PathVariable String eventId) {
        Optional<FailedWebhookEvent> eventOpt = failedWebhookEventService.getFailedEventByEventId(eventId);
        return eventOpt.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Reprocessa um evento falhado
     */
    @PostMapping("/{eventId}/reprocess")
    @Operation(summary = "Reprocessar evento falhado", description = "Tenta reprocessar um evento que falhou anteriormente")
    @ApiResponse(responseCode = "200", description = "Evento reprocessado com sucesso")
    @ApiResponse(responseCode = "404", description = "Evento falhado não encontrado")
    @ApiResponse(responseCode = "500", description = "Erro ao reprocessar evento")
    public ResponseEntity<Map<String, Object>> reprocessFailedEvent(@PathVariable String eventId) {
        try {
            Optional<FailedWebhookEvent> eventOpt = failedWebhookEventService.getFailedEventByEventId(eventId);
            
            if (eventOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Evento falhado não encontrado: " + eventId));
            }
            
            FailedWebhookEvent failedEvent = eventOpt.get();
            log.info("Reprocessando evento falhado: eventId={}, eventType={}, retryCount={}", 
                failedEvent.getEventId(), failedEvent.getEventType(), failedEvent.getRetryCount());
            
            // Reprocessar evento
            failedWebhookEventService.reprocessFailedEvent(failedEvent, webhookService);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Evento reprocessado com sucesso",
                "eventId", eventId,
                "retryCount", failedEvent.getRetryCount()
            ));
            
        } catch (Exception e) {
            log.error("Erro ao reprocessar evento falhado {}: {}", eventId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Erro ao reprocessar evento: " + e.getMessage(),
                    "eventId", eventId
                ));
        }
    }
    
    /**
     * Reprocessa múltiplos eventos falhados
     */
    @PostMapping("/reprocess/batch")
    @Operation(summary = "Reprocessar múltiplos eventos", description = "Reprocessa vários eventos falhados de uma vez")
    @ApiResponse(responseCode = "200", description = "Resultado do reprocessamento em lote")
    public ResponseEntity<Map<String, Object>> reprocessBatch(@RequestParam(required = false) String eventType) {
        try {
            List<FailedWebhookEvent> events;
            if (eventType != null && !eventType.isEmpty()) {
                events = failedWebhookEventService.getFailedEventsByType(eventType);
            } else {
                events = failedWebhookEventService.getAllFailedEvents();
            }
            
            int successCount = 0;
            int errorCount = 0;
            
            for (FailedWebhookEvent event : events) {
                try {
                    failedWebhookEventService.reprocessFailedEvent(event, webhookService);
                    successCount++;
                } catch (Exception e) {
                    log.error("Erro ao reprocessar evento {}: {}", event.getEventId(), e.getMessage());
                    errorCount++;
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "completed",
                "total", events.size(),
                "success", successCount,
                "errors", errorCount
            ));
            
        } catch (Exception e) {
            log.error("Erro ao reprocessar eventos em lote: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Erro ao reprocessar eventos em lote: " + e.getMessage()
                ));
        }
    }
}







