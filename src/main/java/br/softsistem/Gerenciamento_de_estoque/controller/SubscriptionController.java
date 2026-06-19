package br.softsistem.Gerenciamento_de_estoque.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;

import br.softsistem.Gerenciamento_de_estoque.config.PaymentProviderConfig;
import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.AsaasPaymentMode;
import br.softsistem.Gerenciamento_de_estoque.dto.AsaasCheckoutRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.CheckoutResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.CheckoutTransparentRequestDto;
import br.softsistem.Gerenciamento_de_estoque.exception.SubscriptionPersistenceException;
import br.softsistem.Gerenciamento_de_estoque.dto.SubscriptionDto;
import br.softsistem.Gerenciamento_de_estoque.dto.SubscriptionMapper;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.service.SubscriptionService;
import jakarta.validation.Valid;

/**
 * Controller de Assinaturas e Checkout Pro
 *
 * Papel deste controller no fluxo Checkout Pro:
 * - O front-end solicita o inÃ­cio do Checkout enviando o planId
 * - O backend valida o plano e cria a Preference (Checkout Pro) via service
 * - O backend retorna a URL (init_point) para o front realizar o redirecionamento
 * - NÃ£o hÃ¡ redirecionamento pelo backend; o redirecionamento Ã© responsabilidade do front-end
 *
 * Demais endpoints (cancelamento, histÃ³rico, acesso e limites) permanecem coesos ao domÃ­nio de assinaturas.
 */
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;
    private final PaymentProviderConfig paymentProviderConfig;

    public SubscriptionController(SubscriptionService subscriptionService, PaymentProviderConfig paymentProviderConfig) {
        this.subscriptionService = subscriptionService;
        this.paymentProviderConfig = paymentProviderConfig;
    }

    /**
     * Retorna o ID do usuário logado para uso como external_reference no Checkout Transparente.
     * O frontend deve enviar este valor no body do POST /checkout quando usar card_token_id.
     */
    @GetMapping("/current-user-id")
    public ResponseEntity<Map<String, String>> getCurrentUserIdForCheckout() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("externalReference", String.valueOf(userId)));
    }

    /**
     * Obtém a assinatura atual do usuário.
     * Retorna 404 quando não há assinatura (evita DTO vazio que quebra o frontend).
     */
    @GetMapping("/current")
    public ResponseEntity<SubscriptionDto> getCurrentSubscription() {
        Subscription subscription = subscriptionService.getCurrentSubscriptionForUser();
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        SubscriptionDto dto = SubscriptionMapper.toDto(subscription);
        return ResponseEntity.ok(dto);
    }

    /**
     * Sincroniza status da cobrança Asaas e ativa assinatura se o pagamento foi confirmado.
     * Usado após retorno do checkout ou enquanto aguarda webhook.
     */
    @PostMapping("/sync-payment")
    public ResponseEntity<SubscriptionDto> syncAsaasPayment() {
        Subscription subscription = subscriptionService.syncAsaasPaymentForCurrentUser();
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(SubscriptionMapper.toDto(subscription));
    }

    /**
     * Simula confirmação de pagamento no sandbox (mesmo fluxo do sync, explícito para testes).
     */
    @PostMapping("/simulate-payment")
    public ResponseEntity<?> simulateSandboxPayment() {
        try {
            Subscription subscription = subscriptionService.syncAsaasPaymentForCurrentUser();
            if (subscription == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Nenhuma cobrança pendente encontrada"));
            }
            if (subscription.getStatus() != br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus.ACTIVE) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "Pagamento ainda não confirmado. Gere uma cobrança em /assinatura e tente novamente.",
                        "status", subscription.getStatus().name()));
            }
            return ResponseEntity.ok(SubscriptionMapper.toDto(subscription));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro ao simular pagamento: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Inicia o Checkout do Mercado Pago para o plano selecionado
     *
     * Fluxo:
     * 1) Valida o plano no backend (pelo ID do Mercado Pago)
     * 2) Gera a URL direta de assinatura (Direct Link)
     * 3) Retorna a URL para o front redirecionar o usuÃ¡rio
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> startCheckout(
            @RequestParam(required = false) String planId,
            @RequestParam(required = false) String backUrl,
            @RequestParam(required = false) String cardTokenId,
            @RequestBody(required = false) @Valid CheckoutTransparentRequestDto body
    ) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🛒 POST /api/subscription/checkout - Iniciando checkout");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📥 Dados recebidos:");
        log.info("   planId (param): {}", planId);
        log.info("   cardTokenId (param): {}", cardTokenId);
        log.info("   backUrl (param): {}", backUrl);
        log.info("   body: {}", body);
        if (body != null) {
            log.info("   body.planId: {}", body.getPlanId());
            log.info("   body.cardTokenId: {}", body.getCardTokenId() != null ? body.getCardTokenId().substring(0, Math.min(20, body.getCardTokenId().length())) + "..." : "NULL");
            log.info("   body.payerEmail: {}", body.getPayerEmail());
            log.info("   body.externalReference: {}", body.getExternalReference());
        }

        try {
            String effectivePlanId;
            String effectiveCardTokenId;
            String payerEmail = null;

            if (body != null && body.getCardTokenId() != null && !body.getCardTokenId().isBlank()) {
                if (!paymentProviderConfig.isMercadoPago()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Checkout transparente disponível apenas com Mercado Pago"));
                }
                Long currentUserId = SecurityUtils.getCurrentUserId();
                if (currentUserId == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Usuário não autenticado"));
                }
                String extRef = body.getExternalReference();
                if (extRef == null || extRef.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "external_reference é obrigatório no Checkout Transparente"));
                }
                if (!String.valueOf(currentUserId).equals(extRef.trim())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "external_reference não corresponde ao usuário logado"));
                }
                effectivePlanId = body.getPlanId();
                effectiveCardTokenId = body.getCardTokenId();
                payerEmail = body.getPayerEmail();
            } else {
                effectivePlanId = planId != null ? planId : (body != null ? body.getPlanId() : null);
                effectiveCardTokenId = cardTokenId;
            }

            if (effectivePlanId == null || effectivePlanId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "planId é obrigatório"));
            }

            log.info("🚀 Chamando subscriptionService.createSubscriptionForUser");
            log.info("   planId={}, cardTokenPresente={}, payerEmail={}",
                    effectivePlanId, effectiveCardTokenId != null, payerEmail);

            Map<String, Object> response = subscriptionService.createSubscriptionForUser(
                    effectivePlanId, backUrl, effectiveCardTokenId, payerEmail);

            log.info("✅ Resposta do service: {}", response);
            return buildCheckoutResponse(response);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Erro de validação: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("⚠️ Erro de estado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (SubscriptionPersistenceException e) {
            log.error("❌ Erro de persistência: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", e.getMessage(),
                "preapprovalId", e.getPreapprovalId() != null ? e.getPreapprovalId() : ""));
        } catch (Exception e) {
            log.error("❌ Erro inesperado no checkout: {}", e.getMessage(), e);
            String msg = e.getMessage() != null ? e.getMessage() : "";
            // Token do cartão é de uso único; reenviar o mesmo token gera erro no Mercado Pago
            if (msg.contains("Card token was used") || msg.contains("generate new")) {
                return ResponseEntity.badRequest().body(Map.of("error",
                    "Token do cartão já foi utilizado. Recarregue a página e preencha os dados do cartão novamente."));
            }
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro interno: " + msg));
        }
    }

    /**
     * Checkout via Asaas — assinatura recorrente ou cobrança avulsa (Pix/boleto).
     * Body: { planId, cpfCnpj?, paymentMode?: RECURRING|PIX|BOLETO }
     */
    @PostMapping("/checkout/asaas")
    public ResponseEntity<?> startAsaasCheckout(@Valid @RequestBody AsaasCheckoutRequestDto request) {
        return startAsaasCheckoutInternal(request, AsaasPaymentMode.fromString(request.getPaymentMode()));
    }

    /** Cobrança avulsa mensal via Pix (QR Code na resposta, sem redirecionamento). */
    @PostMapping("/checkout/asaas/pix")
    public ResponseEntity<?> startAsaasPixCheckout(@Valid @RequestBody AsaasCheckoutRequestDto request) {
        return startAsaasCheckoutInternal(request, AsaasPaymentMode.PIX);
    }

    /** Cobrança avulsa mensal via boleto (linha digitável e link na resposta). */
    @PostMapping("/checkout/asaas/boleto")
    public ResponseEntity<?> startAsaasBoletoCheckout(@Valid @RequestBody AsaasCheckoutRequestDto request) {
        return startAsaasCheckoutInternal(request, AsaasPaymentMode.BOLETO);
    }

    private ResponseEntity<?> startAsaasCheckoutInternal(AsaasCheckoutRequestDto request, AsaasPaymentMode mode) {
        try {
            Map<String, Object> response = subscriptionService.createSubscriptionForUser(
                    request.getPlanId(), null, null, null, request.getCpfCnpj(), mode);
            return buildCheckoutResponse(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro no checkout Asaas ({}): {}", mode, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<CheckoutResponseDto> buildCheckoutResponse(Map<String, Object> response) {
        String initPoint = (String) response.getOrDefault("checkoutUrl", null);
        String sessionId = (String) response.getOrDefault("sessionId", initPoint);
        Long subscriptionId = (Long) response.getOrDefault("subscriptionId", null);
        String status = (String) response.getOrDefault("status", null);
        Boolean testMode = (Boolean) response.getOrDefault("testMode", false);
        Boolean transparentCheckout = (Boolean) response.getOrDefault("transparentCheckout", false);
        String preapprovalId = (String) response.getOrDefault("preapprovalId", null);
        CheckoutResponseDto dto = new CheckoutResponseDto(initPoint, sessionId, subscriptionId, status, testMode,
                transparentCheckout, preapprovalId);
        dto.setPaymentUrl((String) response.getOrDefault("paymentUrl", initPoint));
        dto.setPaymentProvider((String) response.getOrDefault("paymentProvider", null));
        dto.setAsaasPaymentId((String) response.getOrDefault("asaasPaymentId", null));
        dto.setPaymentMode((String) response.getOrDefault("paymentMode", null));
        dto.setBillingType((String) response.getOrDefault("billingType", null));
        dto.setPixQrCodeImage((String) response.getOrDefault("pixQrCodeImage", null));
        dto.setPixCopyPaste((String) response.getOrDefault("pixCopyPaste", null));
        dto.setPixExpirationDate((String) response.getOrDefault("pixExpirationDate", null));
        dto.setBankSlipUrl((String) response.getOrDefault("bankSlipUrl", null));
        dto.setIdentificationField((String) response.getOrDefault("identificationField", null));
        dto.setDueDate((String) response.getOrDefault("dueDate", null));
        return ResponseEntity.ok(dto);
    }

    /**
     * Cancela a assinatura do usuÃ¡rio
     * Recebe JSON: { "preapprovalId": "2c9..." }
     */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelSubscription(@org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> payload) throws MPException, MPApiException {
        String preapprovalId = (payload != null) ? payload.get("preapprovalId") : null;
        
        if (preapprovalId != null && !preapprovalId.isBlank()) {
            try {
                subscriptionService.cancelSubscriptionByPreapprovalId(preapprovalId);
                return ResponseEntity.ok(Map.of("message", "Assinatura cancelada com sucesso"));
            } catch (IllegalArgumentException e) {
                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            } catch (SecurityException e) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
            }
        }
        
        // Fallback: cancela a assinatura ativa atual (comportamento legado)
        Map<String, String> response = subscriptionService.cancelSubscriptionForUser();
        return ResponseEntity.ok(response);
    }



    /**
     * ObtÃ©m histÃ³rico de assinaturas do usuÃ¡rio
     */
    @GetMapping("/history")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionHistory() {
        List<Subscription> subscriptions = subscriptionService.getSubscriptionHistoryForUser();
        List<SubscriptionDto> dtos = subscriptions.stream()
                .map(SubscriptionMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Verifica se o usuÃ¡rio tem acesso a uma funcionalidade
     */
    @GetMapping("/feature-access")
    public ResponseEntity<Map<String, Boolean>> checkFeatureAccess(@RequestParam String feature) {
        Map<String, Boolean> response = subscriptionService.checkFeatureAccessForUser(feature);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifica limites de uso
     */
    @GetMapping("/usage-limits")
    public ResponseEntity<Map<String, Object>> checkUsageLimits(
            @RequestParam String limitType,
            @RequestParam int currentCount) {
        Map<String, Object> response = subscriptionService.checkUsageLimitsForUser(limitType, currentCount);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lista todas as assinaturas (admin)
     */
    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionDto>> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionService.getAllSubscriptionsForAdmin();
        List<SubscriptionDto> dtos = subscriptions.stream()
                .map(SubscriptionMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    
}