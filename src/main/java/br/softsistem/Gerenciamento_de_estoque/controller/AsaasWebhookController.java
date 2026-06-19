package br.softsistem.Gerenciamento_de_estoque.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.softsistem.Gerenciamento_de_estoque.config.AsaasConfig;
import br.softsistem.Gerenciamento_de_estoque.service.AsaasWebhookService;

/**
 * Webhook do Asaas para confirmação de pagamentos.
 */
@RestController
@RequestMapping({"/webhooks", "/api/webhooks"})
public class AsaasWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AsaasWebhookController.class);

    private final AsaasWebhookService asaasWebhookService;
    private final AsaasConfig asaasConfig;
    private final ObjectMapper objectMapper;

    public AsaasWebhookController(
            AsaasWebhookService asaasWebhookService,
            AsaasConfig asaasConfig,
            ObjectMapper objectMapper) {
        this.asaasWebhookService = asaasWebhookService;
        this.asaasConfig = asaasConfig;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/asaas")
    public ResponseEntity<Map<String, Object>> handleAsaasWebhook(
            @RequestBody String requestBody,
            @RequestHeader(name = "asaas-access-token", required = false) String accessToken) {

        if (requestBody == null || requestBody.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("received", false, "error", "Body vazio"));
        }

        String expectedToken = asaasConfig.getWebhookToken();
        if (expectedToken != null && !expectedToken.isBlank()) {
            if (accessToken == null || !expectedToken.equals(accessToken)) {
                log.warn("Webhook Asaas rejeitado: token inválido");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("received", false));
            }
        } else if (asaasConfig.isProduction()) {
            log.warn("Webhook Asaas em produção sem ASAAS_WEBHOOK_TOKEN configurado");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(requestBody, new TypeReference<>() {});
            asaasWebhookService.processEvent(payload);
            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            log.error("Erro ao processar webhook Asaas: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("received", true, "note", "processamento assíncrono pendente"));
        }
    }
}
