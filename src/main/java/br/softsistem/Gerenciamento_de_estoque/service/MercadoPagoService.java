package br.softsistem.Gerenciamento_de_estoque.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;

import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;

/**
 * Service para integração com a API do Mercado Pago
 * ⭐ VERSÃO FINAL - Funciona com planos do painel E sem plano
 */
@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mercadopago")
public class MercadoPagoService {
    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);

    private static final int FREE_TRIAL_DAYS = 14;

    private final MercadoPagoConfig mercadoPagoConfig;
    private final RestTemplate restTemplate;

    public MercadoPagoService(MercadoPagoConfig mercadoPagoConfig) {
        this.mercadoPagoConfig = mercadoPagoConfig;
        this.restTemplate = new RestTemplate();
    }

    /**
     * ⭐⭐⭐ MÉTODO PRINCIPAL - VERSÃO FINAL ⭐⭐⭐
     *
     * Cria assinatura (Preapproval) com free trial de 14 dias
     *
     * FUNCIONA EM 2 MODOS:
     * 1. COM plano criado no painel (recomendado)
     * 2. SEM plano no painel (cria do zero)
     *
     * @param user                Usuário
     * @param plan                Plano (com ou sem mercadoPagoPreapprovalPlanId)
     * @param localSubscriptionId ID local da assinatura
     * @param cardTokenId         Token do cartão (obrigatório na API do MP; se null, use link direto do plano).
     *                            ATENÇÃO: Token de uso ÚNICO - nunca salvar ou reutilizar.
     * @param payerEmailOverride  E-mail do pagador (opcional; se null, usa user.getEmail())
     * @return Map com id, init_point, status
     */
    public Map<String, Object> createPreapproval(Usuario user, Plan plan, Long localSubscriptionId, String cardTokenId, String payerEmailOverride) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 Criando assinatura com FREE TRIAL");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("👤 Usuário: {}", user.getEmail());
        log.info("📋 Plano: {} (R$ {})", plan.getName(), plan.getPrice());
        log.info("🆔 Subscription ID: {}", localSubscriptionId);

        // ════════════════════════════════════════════════════════════════════
        // VALIDAÇÃO ANTECIPADA DO ACCESS TOKEN
        // ════════════════════════════════════════════════════════════════════
        String accessToken = mercadoPagoConfig.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            String env = mercadoPagoConfig.getEnvironment();
            String varName = mercadoPagoConfig.isProduction()
                    ? "MERCADOPAGO_PROD_ACCESS_TOKEN"
                    : "MERCADOPAGO_TEST_ACCESS_TOKEN";
            String msg = String.format(
                    "Access Token do Mercado Pago não configurado! Ambiente atual: '%s'. "
                  + "Defina a variável de ambiente %s (no SO ou no .env) com um token válido. "
                  + "Gere um novo em: https://www.mercadopago.com.br/developers/panel/app",
                    env, varName);
            log.error("❌ {}", msg);
            throw new IllegalStateException(msg);
        }

        String url = "https://api.mercadopago.com/preapproval";
        // external_reference = ID do usuário logado (obrigatório para vincular assinatura ao usuário no sistema)
        String externalReference = String.valueOf(user.getId());
        // ID do plano no Mercado Pago (plano já criado no painel) — NÃO usar plan.getId() (ID interno)
        String preapprovalPlanIdMercadoPago = plan.getMercadoPagoPreapprovalPlanId();

        // Prioridade: e-mail vindo do front-end (checkout) > usuário logado > env MERCADOPAGO_TEST_PAYER_EMAIL (só fallback em sandbox)
        String payerEmail = (payerEmailOverride != null && !payerEmailOverride.isBlank()) ? payerEmailOverride : user.getEmail();
        if (!mercadoPagoConfig.isProduction()) {
            if (payerEmailOverride != null && !payerEmailOverride.isBlank()) {
                log.info("Sandbox: usando e-mail do pagador enviado pelo front-end: {}", payerEmailOverride);
            } else if (mercadoPagoConfig.getTestPayerEmail() != null && !mercadoPagoConfig.getTestPayerEmail().isBlank()) {
                payerEmail = mercadoPagoConfig.getTestPayerEmail();
                log.info("Sandbox: usando e-mail de pagador de teste configurado (MERCADOPAGO_TEST_PAYER_EMAIL)");
            }
        }
        boolean hasCardToken = cardTokenId != null && !cardTokenId.isBlank();

        Map<String, Object> body = new HashMap<>();
        body.put("payer_email", payerEmail);
        body.put("external_reference", externalReference);
        body.put("back_url", mercadoPagoConfig.getSuccessUrl());

        // COM card_token → "authorized" (pagamento automático via Checkout Transparente)
        // SEM card_token → "pending" (usuário redireciona para init_point do MP)
        if (hasCardToken) {
            body.put("status", "authorized");
            body.put("card_token_id", cardTokenId);
        } else {
            body.put("status", "pending");
        }

        // ════════════════════════════════════════════════════════════════════
        // DECISÃO: Usar plano do painel OU criar do zero?
        // ════════════════════════════════════════════════════════════════════

        if (preapprovalPlanIdMercadoPago != null && !preapprovalPlanIdMercadoPago.isBlank()) {
            // ┌────────────────────────────────────────────────────────────┐
            // │ MODO 1: Plano JÁ EXISTE no painel do Mercado Pago          │
            // │ preapproval_plan_id = ID que vem do MP (plano já criado)   │
            // └────────────────────────────────────────────────────────────┘
            log.info("📦 MODO: Usando plano pré-configurado no Mercado Pago");
            log.info("🔑 preapproval_plan_id (ID do plano no MP): {}", preapprovalPlanIdMercadoPago);

            body.put("preapproval_plan_id", preapprovalPlanIdMercadoPago);
            body.put("reason", plan.getName());

            // ⚠️ ATENÇÃO: Quando tem preapproval_plan_id, NÃO enviar auto_recurring
            // pois causa conflito na API do Mercado Pago (erro 500 em produção)
            // O auto_recurring é lido automaticamente do plano configurado no painel
            log.info("✅ Usando configurações do plano do painel (sem auto_recurring na requisição)");

        } else {
            // ┌────────────────────────────────────────────────────────────┐
            // │ MODO 2: Plano NÃO EXISTE no painel - criar do zero │
            // └────────────────────────────────────────────────────────────┘
            log.info("🔧 MODO: Criando assinatura do zero (sem plano no painel)");

            body.put("reason", plan.getName());
            body.put("auto_recurring", buildAutoRecurringWithTrial(plan));

            log.info("✅ Auto recurring configurado com free trial de {} dias", FREE_TRIAL_DAYS);
        }

        // ════════════════════════════════════════════════════════════════════
        // CHAMADA À API
        // ════════════════════════════════════════════════════════════════════

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📤 Enviando requisição ao Mercado Pago...");
        log.info("📋 status={} ({})", hasCardToken ? "authorized" : "pending",
                hasCardToken ? "Checkout Transparente" : "Redirect para init_point");
        if (hasCardToken) {
            log.info("💳 card_token_id enviado: {}...", cardTokenId.substring(0, Math.min(20, cardTokenId.length())));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // DEBUG: Log da requisição
        log.info("🔍 DEBUG - Requisição para Mercado Pago:");
        log.info("   URL: {}", url);
        log.info("   Ambiente: {}", mercadoPagoConfig.getEnvironment());
        log.info("   Token (mascarado): {}...{}",
                accessToken.substring(0, Math.min(10, accessToken.length())),
                accessToken.substring(Math.max(0, accessToken.length() - 5)));
        log.info("   Headers: {}", headers.entrySet());
        log.info("   Body: {}", body);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            // DEBUG: Log da resposta completa
            log.info("🔍 DEBUG - Resposta do Mercado Pago:");
            log.info("   Response completa: {}", response);

            String preapprovalId = mapGetString(response, "id");
            String initPoint = mapGetString(response, "init_point");
            String sandboxInitPoint = mapGetString(response, "sandbox_init_point");

            // Log das duas URLs se disponíveis
            if (sandboxInitPoint != null) {
                log.info("🔗 Sandbox Checkout URL: {}", sandboxInitPoint);
            }
            log.info("🔗 Production Checkout URL: {}", initPoint);

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ SUCESSO! Assinatura criada");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("🆔 Preapproval ID: {}", preapprovalId);
            // Mostra a URL que será usada de fato baseada no config
            log.info("🔗 URL Selecionada: {}", mercadoPagoConfig.isProduction() ? initPoint
                    : (sandboxInitPoint != null ? sandboxInitPoint : initPoint));
            log.info("📱 O checkout mostrará:");
            log.info("   ✅ 14 dias grátis");
            log.info("   💰 Depois R$ {}/mês", plan.getPrice());
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            return response;

        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            int httpStatus = e.getStatusCode().value();
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("❌ ERRO HTTP ao criar assinatura");
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("Status: {}", e.getStatusCode());
            log.error("Body: {}", responseBody);
            if (httpStatus == 401) {
                String tokenVar = mercadoPagoConfig.isProduction() ? "MERCADOPAGO_PROD_ACCESS_TOKEN" : "MERCADOPAGO_TEST_ACCESS_TOKEN";
                log.error("Dica: 401 = token inválido ou expirado. Ambiente: {}. Verifique a variável {} (SO ou .env) e gere um novo em developers.mercadopago.com.",
                        mercadoPagoConfig.getEnvironment(), tokenVar);
            }
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            String userMessage = parseMercadoPagoErrorMessage(responseBody, httpStatus);
            throw new RuntimeException(userMessage, e);
        } catch (Exception e) {
            log.error("❌ Erro inesperado: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar assinatura no Mercado Pago", e);
        }
    }

    /** Extrai String do Map (API do MP pode retornar id como número no JSON). */
    private static String mapGetString(Map<String, Object> map, String key) {
        Object v = map == null ? null : map.get(key);
        if (v == null) return null;
        if (v instanceof String) return (String) v;
        return String.valueOf(v);
    }

    /**
     * ⭐ Monta configuração auto_recurring com free trial
     * Usado quando cria assinatura do zero (sem plano no painel)
     */
    private Map<String, Object> buildAutoRecurringWithTrial(Plan plan) {
        LocalDate trialEndDate = LocalDate.now().plusDays(FREE_TRIAL_DAYS);
        int billingDay = Math.min(trialEndDate.getDayOfMonth(), 28);

        log.info("📅 Cálculos:");
        log.info("  - Trial: {} dias (termina em {})", FREE_TRIAL_DAYS, trialEndDate);
        log.info("  - Billing day: {} (dia da cobrança mensal)", billingDay);

        Map<String, Object> autoRecurring = new HashMap<>();
        autoRecurring.put("frequency", 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", plan.getPrice());
        autoRecurring.put("currency_id", "BRL");

        // ⭐ START_DATE e END_DATE (conforme documentação Mercado Pago)
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusYears(1); // 1 ano de validade
        autoRecurring.put("start_date", startDate.toString());
        autoRecurring.put("end_date", endDate.toString());

        // ⭐ FREE TRIAL
        Map<String, Object> freeTrial = new HashMap<>();
        freeTrial.put("frequency", FREE_TRIAL_DAYS);
        freeTrial.put("frequency_type", "days");
        autoRecurring.put("free_trial", freeTrial);

        // ⭐ BILLING DAY
        autoRecurring.put("billing_day", billingDay);
        autoRecurring.put("billing_day_proportional", false);

        return autoRecurring;
    }

    /**
     * ⭐ Monta configuração auto_recurring BÁSICA (sem free_trial e billing_day)
     * Usado quando cria assinatura com preapproval_plan_id + card_token_id
     * Conforme documentação: free_trial e billing_day vêm do plano, não da assinatura
     */
    private Map<String, Object> buildAutoRecurringBasic(Plan plan) {
        log.info("📅 Auto recurring básico (sem free_trial/billing_day - vem do plano)");

        Map<String, Object> autoRecurring = new HashMap<>();
        autoRecurring.put("frequency", 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", plan.getPrice());
        autoRecurring.put("currency_id", "BRL");

        // ⭐ START_DATE e END_DATE (obrigatórios conforme documentação)
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusYears(1); // 1 ano de validade
        autoRecurring.put("start_date", startDate.toString());
        autoRecurring.put("end_date", endDate.toString());

        // ⚠️ NÃO incluir free_trial nem billing_day - esses vêm do plano!

        return autoRecurring;
    }

    /**
     * Cancela assinatura no Mercado Pago
     */
    public void cancelPreapproval(String preapprovalId) {
        log.info("🚫 Cancelando assinatura: {}", preapprovalId);

        String url = "https://api.mercadopago.com/preapproval/" + preapprovalId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mercadoPagoConfig.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("status", "cancelled");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            log.info("✅ Assinatura {} cancelada", preapprovalId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                log.warn("⚠️ Assinatura {} não encontrada", preapprovalId);
            } else {
                log.error("❌ Erro ao cancelar: {}", e.getResponseBodyAsString());
                throw new RuntimeException("Erro ao cancelar: " + e.getMessage());
            }
        }
    }

    /**
     * Busca pagamento pelo ID
     */
    public Payment getPayment(Long paymentId) throws MPException, MPApiException {
        PaymentClient client = new PaymentClient();
        log.info("🔍 Buscando pagamento: {}", paymentId);

        try {
            Payment payment = client.get(paymentId);
            log.info("✅ Pagamento: status={} valor={}", payment.getStatus(), payment.getTransactionAmount());
            return payment;
        } catch (MPApiException e) {
            log.error("❌ Erro API: status={}", e.getStatusCode());
            throw e;
        }
    }

    /**
     * Busca merchant order
     */
    public com.mercadopago.resources.merchantorder.MerchantOrder getMerchantOrder(Long orderId)
            throws MPException, MPApiException {
        com.mercadopago.client.merchantorder.MerchantOrderClient client = new com.mercadopago.client.merchantorder.MerchantOrderClient();

        log.info("🔍 Buscando order: {}", orderId);

        try {
            com.mercadopago.resources.merchantorder.MerchantOrder order = client.get(orderId);
            log.info("✅ Order: status={}", order.getOrderStatus());
            return order;
        } catch (MPApiException e) {
            log.error("❌ Erro API: status={}", e.getStatusCode());
            throw e;
        }
    }

    /**
     * Busca assinatura (Preapproval) pelo ID na API do Mercado Pago.
     * Usado no processamento de webhooks subscription_preapproval.
     *
     * @param preapprovalId ID da assinatura no Mercado Pago
     * @return Map com dados do preapproval (id, status, external_reference, payer_email, etc.)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPreapproval(String preapprovalId) {
        log.info("🔍 Buscando preapproval: {}", preapprovalId);
        String url = "https://api.mercadopago.com/preapproval/" + preapprovalId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mercadoPagoConfig.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null) {
                log.info("✅ Preapproval obtido: status={}", body.get("status"));
            }
            return body != null ? body : new HashMap<>();
        } catch (HttpClientErrorException e) {
            log.error("❌ Erro ao buscar preapproval {}: {}", preapprovalId, e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao buscar assinatura no Mercado Pago: " + e.getMessage(), e);
        }
    }

    /**
     * Extrai mensagem amigável do corpo de erro da API do Mercado Pago (JSON).
     * Para o erro "Both payer and collector must be real or test users" retorna orientação em português.
     */
    private String parseMercadoPagoErrorMessage(String responseBody, int statusCode) {
        if (statusCode == 401) {
            String env = mercadoPagoConfig.getEnvironment();
            String varName = mercadoPagoConfig.isProduction()
                    ? "MERCADOPAGO_PROD_ACCESS_TOKEN" : "MERCADOPAGO_TEST_ACCESS_TOKEN";
            String dicaTeste = !mercadoPagoConfig.isProduction()
                    ? " IMPORTANTE: Em ambiente TESTE use o Access Token da seção 'Credenciais de teste' do painel (não use o token de 'Credenciais de produção')."
                    : "";
            return "Credenciais do Mercado Pago inválidas (ambiente: " + env + "). "
                    + "Verifique: (1) Use o ACCESS TOKEN (não a Public Key). "
                    + "(2) Para teste: " + varName + " deve ser o token da seção 'Credenciais de teste'. "
                    + "(3) No .env: " + varName + "=SEU_TOKEN sem aspas e sem espaços. "
                    + "(4) Token expirado? Gere um novo em: https://www.mercadopago.com.br/developers/panel/app > Sua aplicação > Credenciais."
                    + dicaTeste;
        }
        if (statusCode == 400 && responseBody != null
                && responseBody.contains("payer") && responseBody.contains("collector")) {
            return "No ambiente de teste do Mercado Pago, pagador e vendedor precisam ser usuários de teste. "
                    + "Configure MERCADOPAGO_TEST_PAYER_EMAIL com o e-mail de uma conta de teste do tipo 'Comprador' "
                    + "(crie em Suas integrações > Contas de teste no painel do desenvolvedor) ou use esse e-mail no checkout.";
        }
        if (responseBody == null || responseBody.isBlank()) {
            return statusCode == 400 ? "Dados de pagamento inválidos. Verifique o cartão e tente novamente." :
                    "Erro ao processar pagamento. Tente novamente ou use outro cartão.";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<?, ?> json = mapper.readValue(responseBody, Map.class);
            Object message = json.get("message");
            if (message != null && !message.toString().isBlank()) {
                return message.toString();
            }
            Object cause = json.get("cause");
            if (cause instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> causeMap) {
                    Object desc = causeMap.get("description");
                    if (desc != null && !desc.toString().isBlank()) {
                        return desc.toString();
                    }
                }
            }
        } catch (Exception ignored) {
            // fallback
        }
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
    }

    /**
     * Processa webhook do Mercado Pago: busca dados do recurso (payment ou preapproval) e retorna para o handler.
     */
    public Map<String, Object> processWebhookNotification(String dataId, String type)
            throws MPException, MPApiException {
        log.info("📨 Webhook: type={} id={}", type, dataId);

        Map<String, Object> result = new HashMap<>();
        result.put("data_id", dataId);
        result.put("type", type);

        if ("payment".equals(type)) {
            try {
                Payment payment = getPayment(Long.parseLong(dataId));
                result.put("payment", payment);
                result.put("status", payment.getStatus());
            } catch (NumberFormatException e) {
                log.error("❌ ID inválido: {}", dataId);
            }
        } else if ("subscription_preapproval".equalsIgnoreCase(type) || "preapproval".equalsIgnoreCase(type)) {
            try {
                Map<String, Object> preapproval = getPreapproval(dataId);
                result.put("preapproval", preapproval);
            } catch (Exception e) {
                log.error("❌ Erro ao buscar preapproval {}: {}", dataId, e.getMessage());
            }
        } else if ("subscription_authorized_payment".equalsIgnoreCase(type)) {
            try {
                Payment payment = getPayment(Long.parseLong(dataId));
                result.put("payment", payment);
                result.put("status", payment.getStatus());
            } catch (NumberFormatException e) {
                log.error("❌ Payment ID inválido em subscription_authorized_payment: {}", dataId);
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS LEGADOS (Preference - pagamentos únicos)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * @deprecated Use createPreapproval() para assinaturas
     */
    @Deprecated
    public Preference createPreference(Usuario user, Plan plan, boolean withTrial)
            throws MPException, MPApiException {
        log.warn("⚠️ createPreference() é para pagamentos ÚNICOS!");
        log.warn("⚠️ Para ASSINATURAS use createPreapproval()");

        PreferenceClient client = new PreferenceClient();

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title(plan.getName())
                .description(plan.getDescription())
                .quantity(1)
                .unitPrice(plan.getPrice())
                .currencyId("BRL")
                .build();

        List<PreferenceItemRequest> items = new ArrayList<>();
        items.add(item);

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items)
                .payer(com.mercadopago.client.preference.PreferencePayerRequest.builder()
                        .email(user.getEmail())
                        .name(user.getUsername())
                        .build())
                .backUrls(com.mercadopago.client.preference.PreferenceBackUrlsRequest.builder()
                        .success(mercadoPagoConfig.getSuccessUrl())
                        .failure(mercadoPagoConfig.getCancelUrl())
                        .pending(mercadoPagoConfig.getPendingUrl())
                        .build())
                .notificationUrl(mercadoPagoConfig.getWebhookUrl())
                .externalReference(user.getId() + ":" + plan.getId())
                .autoReturn("approved")
                .build();

        return client.create(preferenceRequest);
    }

    @Deprecated
    public Preference createPreference(Usuario user, Plan plan, boolean withTrial, Long subscriptionId)
            throws MPException, MPApiException {
        return createPreference(user, plan, withTrial);
    }

    @Deprecated
    public Preference createRecurringPreference(Usuario user, Plan plan, boolean withTrial)
            throws MPException, MPApiException {
        return createPreference(user, plan, withTrial);
    }

    public Preference getPreference(String preferenceId) throws MPException, MPApiException {
        PreferenceClient client = new PreferenceClient();
        return client.get(preferenceId);
    }
}