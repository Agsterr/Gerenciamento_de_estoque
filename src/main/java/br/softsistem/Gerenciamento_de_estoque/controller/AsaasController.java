package br.softsistem.Gerenciamento_de_estoque.controller;



import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;



import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;



import br.softsistem.Gerenciamento_de_estoque.config.AsaasConfig;

import br.softsistem.Gerenciamento_de_estoque.service.AsaasService;



/**

 * Informações públicas do Asaas para o frontend (sem expor a API key).

 */

@RestController

@RequestMapping("/api/asaas")

public class AsaasController {



    private final AsaasConfig asaasConfig;

    private final AsaasService asaasService;



    public AsaasController(AsaasConfig asaasConfig, AsaasService asaasService) {

        this.asaasConfig = asaasConfig;

        this.asaasService = asaasService;

    }



    @GetMapping("/config")

    public ResponseEntity<Map<String, Object>> getPublicConfig() {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("environment", asaasConfig.isSandbox() ? "sandbox" : "production");

        body.put("testMode", asaasConfig.isSandbox());

        body.put("configured", asaasConfig.isConfigured());

        body.put("apiKeyValid", asaasService.isApiKeyValid());
        if (asaasConfig.isConfigured() && !asaasService.isApiKeyValid()) {
            body.put("hint", asaasConfig.isSandbox()
                    ? "Gere uma chave em https://sandbox.asaas.com (prefixo $aact_hmlg_) e salve em docker/secrets/asaas_sandbox_api_key.txt"
                    : "Use chave de produção ($aact_prod_) em ASAAS_PROD_API_KEY ou arquivo equivalente");
        }

        body.put("checkoutEndpoint", "/api/subscription/checkout/asaas");
        body.put("checkoutPixEndpoint", "/api/subscription/checkout/asaas/pix");
        body.put("checkoutBoletoEndpoint", "/api/subscription/checkout/asaas/boleto");
        body.put("paymentModes", List.of(
                Map.of("id", "RECURRING", "label", "Assinatura recorrente", "description", "Cartão ou Pix recorrente na página do Asaas"),
                Map.of("id", "PIX", "label", "Pix mensal", "description", "Cobrança avulsa com QR Code — renove manualmente a cada mês"),
                Map.of("id", "BOLETO", "label", "Boleto mensal", "description", "Cobrança avulsa com boleto — renove manualmente a cada mês")
        ));
        String webhookUrl = asaasConfig.getWebhookUrl();
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            body.put("webhookUrl", webhookUrl);
        }
        body.put("webhookConfigured", asaasConfig.getWebhookToken() != null && !asaasConfig.getWebhookToken().isBlank());

        return ResponseEntity.ok(body);

    }

}


