package br.softsistem.Gerenciamento_de_estoque.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import br.softsistem.Gerenciamento_de_estoque.config.AsaasConfig;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;

/**
 * Integração com a API do Asaas (clientes, assinaturas recorrentes e cobranças).
 */
@Service
public class AsaasService {

    private static final Logger log = LoggerFactory.getLogger(AsaasService.class);

    /** CPF válido para testes no sandbox quando o usuário não informou documento. */
    private static final String SANDBOX_DEFAULT_CPF = "24971563792";

    private final AsaasConfig asaasConfig;
    private final RestClient restClient;

    public AsaasService(AsaasConfig asaasConfig) {
        this.asaasConfig = asaasConfig;
        this.restClient = RestClient.builder()
                .baseUrl(asaasConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "GerenciamentoEstoque/1.0")
                .build();
    }

    /**
     * Cria assinatura recorrente mensal no Asaas e retorna a primeira cobrança (invoiceUrl).
     */
    public Map<String, Object> createRecurringSubscription(Usuario user, Plan plan, Subscription subscription, String cpfCnpj) {
        validateAsaasConfigured();

        String customerId = ensureCustomer(user, cpfCnpj);
        LocalDate nextDueDate = resolveNextDueDate(subscription);

        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", "UNDEFINED");
        body.put("value", plan.getPrice().doubleValue());
        body.put("nextDueDate", nextDueDate.toString());
        body.put("cycle", "MONTHLY");
        body.put("description", "Assinatura mensal " + plan.getName() + " — R$ " + plan.getPrice());
        body.put("externalReference", "sub:" + subscription.getId());

        log.info("Criando assinatura recorrente Asaas subscriptionLocal={} customer={} valor={} nextDueDate={}",
                subscription.getId(), customerId, plan.getPrice(), nextDueDate);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> asaasSubscription = restClient.post()
                    .uri("/subscriptions")
                    .header("access_token", asaasConfig.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (asaasSubscription == null || asaasSubscription.get("id") == null) {
                throw new RuntimeException("Resposta vazia do Asaas ao criar assinatura recorrente");
            }

            String asaasSubscriptionId = String.valueOf(asaasSubscription.get("id"));
            log.info("Assinatura Asaas criada: id={}", asaasSubscriptionId);

            Map<String, Object> firstPayment = waitForFirstSubscriptionPayment(asaasSubscriptionId);
            Map<String, Object> response = new HashMap<>(asaasSubscription);
            response.put("asaasSubscriptionId", asaasSubscriptionId);
            if (firstPayment != null) {
                response.put("firstPayment", firstPayment);
                response.put("id", firstPayment.get("id"));
                response.put("invoiceUrl", resolveInvoiceUrl(firstPayment));
                response.put("bankSlipUrl", firstPayment.get("bankSlipUrl"));
            }
            return response;
        } catch (RestClientResponseException e) {
            log.error("Erro Asaas ao criar assinatura recorrente: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao criar assinatura recorrente no Asaas: " + extractErrorMessage(e), e);
        }
    }

    /**
     * Cobrança avulsa mensal (Pix ou boleto) — sem assinatura recorrente no Asaas.
     *
     * @param billingType PIX ou BOLETO
     */
    public Map<String, Object> createMonthlyCharge(Usuario user, Plan plan, Subscription subscription,
            String cpfCnpj, String billingType) {
        validateAsaasConfigured();
        if (!"PIX".equals(billingType) && !"BOLETO".equals(billingType)) {
            throw new IllegalArgumentException("billingType deve ser PIX ou BOLETO");
        }

        String customerId = ensureCustomer(user, cpfCnpj);
        LocalDate dueDate = resolveChargeDueDate(subscription);

        Map<String, Object> body = new HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", billingType);
        body.put("value", plan.getPrice().doubleValue());
        body.put("dueDate", dueDate.toString());
        body.put("description", "Mensalidade " + plan.getName() + " — R$ " + plan.getPrice());
        body.put("externalReference", "sub:" + subscription.getId());

        if (shouldIncludePaymentCallback()) {
            Map<String, Object> callback = new HashMap<>();
            String base = asaasConfig.getAppPublicUrl().replaceAll("/$", "");
            callback.put("successUrl", base + "/subscription/success");
            callback.put("autoRedirect", true);
            body.put("callback", callback);
        }

        log.info("Criando cobrança avulsa Asaas billingType={} subscription={} customer={} valor={} dueDate={}",
                billingType, subscription.getId(), customerId, plan.getPrice(), dueDate);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/payments")
                    .header("access_token", asaasConfig.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("id") == null) {
                throw new RuntimeException("Resposta vazia do Asaas ao criar cobrança avulsa");
            }

            String paymentId = String.valueOf(response.get("id"));
            log.info("Cobrança avulsa Asaas criada: id={} billingType={}", paymentId, billingType);

            Map<String, Object> enriched = new HashMap<>(response);
            enriched.put("billingType", billingType);
            enriched.put("dueDate", dueDate.toString());

            if ("PIX".equals(billingType)) {
                mergePixDetails(enriched, getPixQrCode(paymentId));
            } else {
                enriched.put("bankSlipUrl", response.get("bankSlipUrl"));
                enriched.put("identificationField", response.get("identificationField"));
                enriched.put("invoiceUrl", resolveInvoiceUrl(response));
            }
            return enriched;
        } catch (RestClientResponseException e) {
            log.error("Erro Asaas ao criar cobrança avulsa: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao criar cobrança no Asaas: " + extractErrorMessage(e), e);
        }
    }

    /**
     * Consulta QR Code Pix de uma cobrança (GET /payments/{id}/pixQrCode).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPixQrCode(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("ID da cobrança Asaas é obrigatório");
        }
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/payments/{id}/pixQrCode", paymentId)
                    .header("access_token", asaasConfig.getApiKey())
                    .retrieve()
                    .body(Map.class);
            return response != null ? response : Map.of();
        } catch (RestClientResponseException e) {
            log.error("Erro ao obter QR Code Pix {}: status={} body={}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao obter QR Code Pix no Asaas: " + extractErrorMessage(e), e);
        }
    }

    private void mergePixDetails(Map<String, Object> target, Map<String, Object> pixQr) {
        if (pixQr == null || pixQr.isEmpty()) {
            return;
        }
        target.put("pixQrCodeImage", pixQr.get("encodedImage"));
        target.put("pixCopyPaste", pixQr.get("payload"));
        target.put("pixExpirationDate", pixQr.get("expirationDate"));
    }

    private LocalDate resolveChargeDueDate(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.TRIAL
                && subscription.getTrialEnd() != null
                && subscription.getTrialEnd().toLocalDate().isAfter(LocalDate.now())) {
            return subscription.getTrialEnd().toLocalDate();
        }
        return LocalDate.now().plusDays(3);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSubscriptionPayments(String asaasSubscriptionId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/subscriptions/{id}/payments", asaasSubscriptionId)
                    .header("access_token", asaasConfig.getApiKey())
                    .retrieve()
                    .body(Map.class);
            return extractDataList(response);
        } catch (RestClientResponseException e) {
            log.error("Erro ao listar cobranças da assinatura {}: {}", asaasSubscriptionId, e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao consultar cobranças da assinatura no Asaas", e);
        }
    }

    public void cancelRecurringSubscription(String asaasSubscriptionId) {
        if (asaasSubscriptionId == null || asaasSubscriptionId.isBlank()) {
            return;
        }
        try {
            restClient.delete()
                    .uri("/subscriptions/{id}", asaasSubscriptionId)
                    .header("access_token", asaasConfig.getApiKey())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Assinatura recorrente Asaas removida: {}", asaasSubscriptionId);
        } catch (RestClientResponseException e) {
            log.warn("Erro ao cancelar assinatura Asaas {}: {}", asaasSubscriptionId, extractErrorMessage(e));
            throw new RuntimeException("Erro ao cancelar assinatura no Asaas: " + extractErrorMessage(e), e);
        }
    }

    public String resolveInvoiceUrl(Map<String, Object> payment) {
        if (payment == null) {
            return null;
        }
        String invoiceUrl = mapGetString(payment, "invoiceUrl");
        if (invoiceUrl != null && !invoiceUrl.isBlank()) {
            return invoiceUrl;
        }
        return mapGetString(payment, "bankSlipUrl");
    }

    private Map<String, Object> waitForFirstSubscriptionPayment(String asaasSubscriptionId) {
        for (int attempt = 0; attempt < 6; attempt++) {
            List<Map<String, Object>> payments = getSubscriptionPayments(asaasSubscriptionId);
            if (!payments.isEmpty()) {
                return payments.get(0);
            }
            try {
                Thread.sleep(400L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("Nenhuma cobrança gerada ainda para assinatura Asaas {}", asaasSubscriptionId);
        return null;
    }

    private LocalDate resolveNextDueDate(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.TRIAL
                && subscription.getTrialEnd() != null
                && subscription.getTrialEnd().toLocalDate().isAfter(LocalDate.now())) {
            return subscription.getTrialEnd().toLocalDate();
        }
        return LocalDate.now().plusDays(1);
    }

    private void validateAsaasConfigured() {
        if (!asaasConfig.isConfigured()) {
            throw new IllegalStateException("Asaas não configurado. Defina ASAAS_SANDBOX_API_KEY ou docker/secrets/asaas_sandbox_api_key.txt");
        }
        if (!isApiKeyValid()) {
            String hint = asaasConfig.isSandbox()
                    ? " Gere uma chave Sandbox em https://sandbox.asaas.com (Integrações > Chaves de API). O prefixo deve ser $aact_hmlg_."
                    : " Use uma chave de produção com prefixo $aact_prod_.";
            throw new IllegalStateException(
                    "Chave da API Asaas inválida, expirada ou de ambiente errado." + hint
                            + " Atualize docker/secrets/asaas_sandbox_api_key.txt e recrie o container.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataList(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }
        Object data = response.get("data");
        if (!(data instanceof List<?> rawList)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private static String mapGetString(Map<String, Object> map, String key) {
        Object value = map != null ? map.get(key) : null;
        return value != null ? String.valueOf(value) : null;
    }

    /** Valida a chave de API com uma chamada leve ao Asaas. */
    public boolean isApiKeyValid() {
        if (!asaasConfig.isConfigured()) {
            return false;
        }
        try {
            restClient.get()
                    .uri("/myAccount/status")
                    .header("access_token", asaasConfig.getApiKey())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                log.warn("Chave Asaas inválida ou ambiente incorreto: {}", extractErrorMessage(e));
                return false;
            }
            log.warn("Resposta inesperada ao validar chave Asaas (HTTP {}): {}", status, extractErrorMessage(e));
            return false;
        } catch (Exception e) {
            log.warn("Falha ao validar chave Asaas: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Consulta status de uma cobrança no Asaas.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPayment(String paymentId) {
        try {
            return restClient.get()
                    .uri("/payments/{id}", paymentId)
                    .header("access_token", asaasConfig.getApiKey())
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.error("Erro ao consultar cobrança {}: {}", paymentId, e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao consultar cobrança no Asaas", e);
        }
    }

    /**
     * Confirma pagamento no sandbox via API (quando não há botão na interface do Asaas).
     * POST /v3/sandbox/payment/{id}/confirm
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> confirmSandboxPayment(String paymentId) {
        if (!asaasConfig.isSandbox()) {
            throw new IllegalStateException("Simulação de pagamento só está disponível no ambiente sandbox");
        }
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("ID da cobrança Asaas é obrigatório");
        }
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/sandbox/payment/{id}/confirm", paymentId)
                    .header("access_token", asaasConfig.getApiKey())
                    .body(Map.of())
                    .retrieve()
                    .body(Map.class);
            log.info("Cobrança sandbox confirmada via API: id={} status={}",
                    paymentId, response != null ? response.get("status") : null);
            return response != null ? response : Map.of();
        } catch (RestClientResponseException e) {
            log.error("Erro ao confirmar cobrança sandbox {}: status={} body={}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao simular pagamento no Asaas: " + extractErrorMessage(e), e);
        }
    }

    public String ensureCustomer(Usuario user, String cpfCnpj) {
        if (user.getAsaasCustomerId() != null && !user.getAsaasCustomerId().isBlank()) {
            return user.getAsaasCustomerId();
        }

        String document = resolveCpfCnpj(user, cpfCnpj);

        Map<String, Object> body = new HashMap<>();
        body.put("name", user.getUsername() != null ? user.getUsername() : user.getEmail());
        body.put("email", user.getEmail());
        body.put("cpfCnpj", document);
        body.put("externalReference", "user:" + user.getId());
        body.put("notificationDisabled", true);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/customers")
                    .header("access_token", asaasConfig.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("id") == null) {
                throw new RuntimeException("Asaas não retornou ID do cliente");
            }

            String customerId = String.valueOf(response.get("id"));
            user.setAsaasCustomerId(customerId);
            if (cpfCnpj != null && !cpfCnpj.isBlank()) {
                user.setCpfCnpj(sanitizeDocument(cpfCnpj));
            }
            log.info("Cliente Asaas criado: {} para user={}", customerId, user.getId());
            return customerId;
        } catch (RestClientResponseException e) {
            log.error("Erro Asaas ao criar cliente: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao criar cliente no Asaas: " + extractErrorMessage(e), e);
        }
    }

    private String resolveCpfCnpj(Usuario user, String cpfCnpj) {
        if (cpfCnpj != null && !cpfCnpj.isBlank()) {
            return sanitizeDocument(cpfCnpj);
        }
        if (user.getCpfCnpj() != null && !user.getCpfCnpj().isBlank()) {
            return sanitizeDocument(user.getCpfCnpj());
        }
        if (asaasConfig.isSandbox()) {
            return SANDBOX_DEFAULT_CPF;
        }
        throw new IllegalArgumentException("CPF/CNPJ é obrigatório para gerar cobrança. Informe no checkout.");
    }

    private static String sanitizeDocument(String doc) {
        return doc.replaceAll("[^0-9]", "");
    }

    /**
     * Callback exige domínio cadastrado em Minha Conta → Informações no Asaas.
     * Desabilitado por padrão; localhost e trycloudflare nunca enviam callback.
     */
    private boolean shouldIncludePaymentCallback() {
        if (!asaasConfig.isPaymentCallbackEnabled()) {
            return false;
        }
        String url = asaasConfig.getAppPublicUrl();
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            host = host.toLowerCase();
            if ("localhost".equals(host) || host.startsWith("127.")) {
                return false;
            }
            if (host.endsWith(".trycloudflare.com")) {
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("URL pública inválida para callback Asaas: {}", url);
            return false;
        }
    }

    private static String extractErrorMessage(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            String description = extractAsaasErrorDescription(body);
            if (description != null && !description.isBlank()) {
                return description;
            }
            return body.length() > 300 ? body.substring(0, 300) + "..." : body;
        }
        return e.getMessage();
    }

    private static String extractAsaasErrorDescription(String body) {
        int idx = body.indexOf("\"description\"");
        if (idx < 0) {
            return null;
        }
        int start = body.indexOf(':', idx);
        if (start < 0) {
            return null;
        }
        start = body.indexOf('"', start + 1);
        if (start < 0) {
            return null;
        }
        int end = body.indexOf('"', start + 1);
        if (end < 0) {
            return null;
        }
        return body.substring(start + 1, end);
    }

    public boolean isPaymentConfirmed(Map<String, Object> payment) {
        if (payment == null) return false;
        Object status = payment.get("status");
        if (status == null) return false;
        String s = status.toString();
        return "RECEIVED".equals(s) || "CONFIRMED".equals(s);
    }

    public BigDecimal extractValue(Map<String, Object> payment) {
        Object value = payment.get("value");
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (value != null) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO;
    }
}
