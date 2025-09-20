package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.StripeConfig;
import br.softsistem.Gerenciamento_de_estoque.service.WebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller para processar webhooks do Stripe
 */
@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Processamento de eventos do Stripe")
public class WebhookController {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    
    // Tolerância para verificação de assinatura (5 minutos)
    private static final long WEBHOOK_TOLERANCE_SECONDS = 300L;
    
    // Cache para idempotência - em produção, usar Redis ou banco de dados
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();
    
    private final WebhookService webhookService;
    private final StripeConfig stripeConfig;
    
    @Autowired
    public WebhookController(WebhookService webhookService, StripeConfig stripeConfig) {
        this.webhookService = webhookService;
        this.stripeConfig = stripeConfig;
    }
    
    /**
     * Endpoint para processar webhooks do Stripe
     */
    @PostMapping("/stripe")
    @Operation(
        summary = "Processar webhook do Stripe",
        description = "Recebe e processa eventos de webhook do Stripe"
    )
    @ApiResponse(responseCode = "200", description = "Evento processado com sucesso")
    @ApiResponse(responseCode = "400", description = "Evento inválido ou erro de assinatura")
    @ApiResponse(responseCode = "500", description = "Erro interno ao processar evento")
    public ResponseEntity<Map<String, String>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        log.info("Recebido webhook do Stripe");
        
        Event event;
        
        try {
            // Verificar assinatura do webhook com tolerância
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret(), WEBHOOK_TOLERANCE_SECONDS);
            log.info("Webhook verificado com sucesso. Tipo: {}, ID: {}", event.getType(), event.getId());
            
        } catch (SignatureVerificationException e) {
            log.error("Falha na verificação da assinatura do webhook: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Assinatura inválida");
            return ResponseEntity.badRequest().body(error);
            
        } catch (Exception e) {
            log.error("Erro ao processar webhook: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erro ao processar webhook");
            return ResponseEntity.badRequest().body(error);
        }
        
        // Verificar idempotência
        String eventId = event.getId();
        if (processedEvents.contains(eventId)) {
            log.info("Evento {} já foi processado anteriormente, ignorando", eventId);
            Map<String, String> response = new HashMap<>();
            response.put("status", "already_processed");
            response.put("event_id", eventId);
            response.put("event_type", event.getType());
            return ResponseEntity.ok(response);
        }
        
        // Processar evento baseado no tipo
        try {
            webhookService.processStripeEvent(event);
            
            // Marcar evento como processado
            processedEvents.add(eventId);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("event_id", eventId);
            response.put("event_type", event.getType());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao processar evento {}: {}", event.getType(), e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erro interno ao processar evento");
            error.put("event_id", eventId);
            error.put("event_type", event.getType());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Endpoint de teste para verificar se o webhook está funcionando
     */
    @GetMapping("/stripe/test")
    @Operation(
        summary = "Testar webhook",
        description = "Endpoint de teste para verificar se o webhook está funcionando"
    )
    @ApiResponse(responseCode = "200", description = "Webhook funcionando")
    public ResponseEntity<Map<String, String>> testWebhook() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Webhook endpoint está funcionando");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}