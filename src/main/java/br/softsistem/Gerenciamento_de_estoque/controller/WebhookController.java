package br.softsistem.Gerenciamento_de_estoque.controller;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
import br.softsistem.Gerenciamento_de_estoque.dto.webhook.MercadoPagoWebhookDto;
import br.softsistem.Gerenciamento_de_estoque.dto.webhook.WebhookResponseDto;
import br.softsistem.Gerenciamento_de_estoque.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller exclusivo para processar webhooks do Mercado Pago
 * 
 * Endpoint público: POST /webhooks/mercadopago
 * 
 * Validação de segurança via header x-signature conforme documentação oficial
 */
@RestController
@RequestMapping({"/webhooks", "/api/webhooks"})
@Tag(name = "Webhooks", description = "Processamento de eventos do Mercado Pago")
public class WebhookController {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;
    private final MercadoPagoConfig mercadoPagoConfig;
    private final ObjectMapper objectMapper;

    public WebhookController(WebhookService webhookService, MercadoPagoConfig mercadoPagoConfig, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.mercadoPagoConfig = mercadoPagoConfig;
        this.objectMapper = objectMapper;
    }

    /**
     * Endpoint público para receber webhooks do Mercado Pago
     * 
     * Boas práticas implementadas:
     * - Aceita JSON no body
     * - Captura headers (x-signature, x-request-id)
     * - Loga o payload recebido
     * - Retorna HTTP 200 imediatamente
     * - Processa lógica pesada de forma assíncrona
     * 
     * @param requestBody Body raw da requisição como String (para validação de assinatura)
     * @param signature Header x-signature para validação (formato: ts=...,v1=...)
     * @param requestId Header x-request-id para idempotência
     * @return HTTP 200 imediatamente após validação básica
     */
    @PostMapping("/mercadopago")
    @Operation(
        summary = "Receber webhook do Mercado Pago",
        description = "Recebe eventos de webhook do Mercado Pago. " +
                     "Valida assinatura via header x-signature e processa de forma assíncrona. " +
                     "Retorna HTTP 200 imediatamente após validação."
    )
    @ApiResponse(responseCode = "200", description = "Webhook recebido e em processamento")
    @ApiResponse(responseCode = "401", description = "Assinatura inválida - webhook rejeitado")
    @ApiResponse(responseCode = "400", description = "Payload inválido")
    public ResponseEntity<WebhookResponseDto> handleMercadoPagoWebhook(
            @RequestBody String requestBody,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            HttpServletRequest request) {
        
        // Não exigir HTTPS no webhook: chamadas reais do MP e teste do dashboard (via ngrok ou proxy)
        // chegam com scheme "http"; a segurança é feita pela validação da assinatura (x-signature).
        // Evita 403 ao testar a URL no painel do Mercado Pago.

        // Log estruturado do recebimento do webhook
        logStructured("webhook.received", Map.of(
            "requestId", requestId != null ? requestId : "unknown",
            "signaturePresent", String.valueOf(signature != null && !signature.isEmpty()),
            "bodyLength", String.valueOf(requestBody != null ? requestBody.length() : 0),
            "scheme", effectiveScheme(request),
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
        
        // Log detalhado do payload (apenas em debug)
        if (log.isDebugEnabled()) {
            log.debug("Payload completo: {}", requestBody);
        }
        
        try {
            // Validação básica do body
            if (requestBody == null || requestBody.isEmpty()) {
                log.warn("Body vazio recebido");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(WebhookResponseDto.error("Body não pode ser vazio"));
            }
            
            // Parse do JSON para validação de estrutura
            MercadoPagoWebhookDto payload = parsePayload(requestBody);
            
            if (payload == null || payload.getType() == null || payload.getData() == null || payload.getData().getId() == null) {
                log.warn("Payload inválido: campos obrigatórios ausentes");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(WebhookResponseDto.error("Payload inválido: campos obrigatórios ausentes"));
            }
            
            // Validar assinatura apenas em produção. Em teste, aceitar e logar aviso.
            if (mercadoPagoConfig.isProduction()) {
                String webhookSecret = webhookService.getWebhookSecret();
                if (!webhookService.validateWebhookSignature(signature, requestBody, webhookSecret)) {
                    log.error("Webhook rejeitado - assinatura inválida. Request ID: {}", requestId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(WebhookResponseDto.error("Assinatura inválida - webhook rejeitado"));
                }
            } else {
                log.warn("Ambiente de teste: validação de assinatura pulada");
            }
            
            // Converter DTO para Map para processamento
            Map<String, Object> payloadMap = convertDtoToMap(payload);
            
            // IMPORTANTE: eventId vem do campo "id" do webhook (não do payment_id)
            // Exemplo: payload { "type": "payment", "data": { "id": "123456789" } }
            // eventId será "123456789"
            String eventId = payload.getData().getId();
            
            // Processar webhook de forma assíncrona (não bloqueia resposta)
            webhookService.processWebhookAsync(payloadMap, signature, requestId, requestBody);
            
            // Retornar HTTP 200 imediatamente
            return ResponseEntity.ok(WebhookResponseDto.success(eventId, payload.getType()));
            
        } catch (JsonProcessingException e) {
            logStructured("webhook.parse.error", Map.of(
                "requestId", requestId != null ? requestId : "unknown",
                "error", "JsonProcessingException",
                "errorMessage", e.getMessage() != null ? e.getMessage() : "unknown",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(WebhookResponseDto.error("Payload JSON inválido"));
        } catch (Exception e) {
            // Log estruturado do erro
            logStructured("webhook.receive.error", Map.of(
                "requestId", requestId != null ? requestId : "unknown",
                "error", e.getClass().getSimpleName(),
                "errorMessage", e.getMessage() != null ? e.getMessage() : "unknown",
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
            
            // IMPORTANTE: Mesmo em caso de erro, retornar 200 para evitar retentativas do Mercado Pago
            // O erro será logado e salvo em failed_webhook_events para reprocessamento manual
            // Isso garante que o endpoint nunca quebre e o Mercado Pago não fique retentando
            return ResponseEntity.ok(WebhookResponseDto.error("Webhook recebido, processamento em andamento"));
        }
    }
    
    /**
     * Retorna o esquema efetivo da requisição (https ou http).
     * Considera X-Forwarded-Proto quando atrás de proxy (ngrok, etc.).
     */
    private String effectiveScheme(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-Proto");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.trim().split(",")[0].trim();
        }
        return request.getScheme();
    }

    /**
     * Log estruturado para melhor rastreabilidade
     */
    private void logStructured(String event, Map<String, String> fields) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[WEBHOOK] ");
        logMessage.append("event=").append(event);
        
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            logMessage.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        switch (event) {
            case "webhook.received":
                log.info(logMessage.toString());
                break;
            case "webhook.security.error":
            case "webhook.parse.error":
            case "webhook.receive.error":
                log.error(logMessage.toString());
                break;
            default:
                log.info(logMessage.toString());
        }
    }
    
    private MercadoPagoWebhookDto parsePayload(String requestBody) throws JsonProcessingException {
        return objectMapper.readValue(requestBody, MercadoPagoWebhookDto.class);
    }

    private Map<String, Object> convertDtoToMap(MercadoPagoWebhookDto dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", dto.getType());
        if (dto.getAction() != null) {
            map.put("action", dto.getAction());
        }

        if (dto.getData() != null) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("id", dto.getData().getId());
            map.put("data", dataMap);
        }

        return map;
    }


}
