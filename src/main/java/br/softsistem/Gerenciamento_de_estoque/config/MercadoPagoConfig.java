package br.softsistem.Gerenciamento_de_estoque.config;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuração do Mercado Pago focada em Checkout Pro
 *
 * Regra de ouro:
 * - Checkout Pro → Access Token apenas
 * - Public Key → NÃO utilizada
 * - Frontend → apenas redireciona para a checkoutUrl gerada pelo backend
 *
 * Esta classe:
 * - NÃO gera checkoutUrl
 * - NÃO contém lógica de pagamento
 * - Fornece Access Token (test/prod) e URLs (success/cancel/pending/webhook)
 */
@Configuration
public class MercadoPagoConfig {
    
    private static final Logger log = LoggerFactory.getLogger(MercadoPagoConfig.class);
    
    private final String environment;
    private final String publicUrl;
    private final String testAccessToken;
    private final String prodAccessToken;
    private final String testPublicKey;
    private final String prodPublicKey;
    private final String webhookSecret;
    private final String successUrl;
    private final String cancelUrl;
    private final String pendingUrl;
    private final String subscriptionCheckoutBaseUrlOverride;
    /** E-mail do usuário de teste (Comprador) para uso em sandbox. Quando definido, substitui o e-mail do usuário ao criar preapproval. */
    private final String testPayerEmail;
    private final String paymentProvider;

    /**
     * Construtor com injeção de dependências
     */
    public MercadoPagoConfig(
            @Value("${mercadopago.environment:test}") String environment,
            @Value("${app.public-url:}") String publicUrl,
            @Value("${mercadopago.test.access.token:}") String testAccessToken,
            @Value("${mercadopago.prod.access.token:}") String prodAccessToken,
            @Value("${mercadopago.test.public.key:}") String testPublicKey,
            @Value("${mercadopago.prod.public.key:}") String prodPublicKey,
            @Value("${mercadopago.webhook.secret:}") String webhookSecret,
            @Value("${mercadopago.success.url:https://gerenciamento-de-estoque-front.vercel.app/subscription/success}") String successUrl,
            @Value("${mercadopago.cancel.url:https://gerenciamento-de-estoque-front.vercel.app/subscription/cancel}") String cancelUrl,
            @Value("${mercadopago.pending.url:https://gerenciamento-de-estoque-front.vercel.app/subscription/pending}") String pendingUrl,
            @Value("${mercadopago.subscription.checkout.base.url:}") String subscriptionCheckoutBaseUrlOverride,
            @Value("${mercadopago.test.payer.email:}") String testPayerEmail,
            @Value("${app.payment.provider:asaas}") String paymentProvider
    ) {
        // Só "production" ou "prod" = produção; qualquer outro valor (incl. vazio) = teste
        String raw = (environment != null ? environment.trim() : "").toLowerCase();
        this.environment = ("production".equals(raw) || "prod".equals(raw)) ? raw : "test";
        this.publicUrl = publicUrl;
        this.testAccessToken = (testAccessToken != null ? testAccessToken.trim() : "").replaceAll("\\s+", "");
        this.prodAccessToken = (prodAccessToken != null ? prodAccessToken.trim() : "").replaceAll("\\s+", "");
        this.testPublicKey = testPublicKey != null ? testPublicKey.trim() : "";
        this.prodPublicKey = prodPublicKey != null ? prodPublicKey.trim() : "";
        this.webhookSecret = webhookSecret;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.pendingUrl = pendingUrl;
        this.subscriptionCheckoutBaseUrlOverride = subscriptionCheckoutBaseUrlOverride != null ? subscriptionCheckoutBaseUrlOverride.trim() : "";
        this.testPayerEmail = testPayerEmail != null ? testPayerEmail.trim() : "";
        this.paymentProvider = paymentProvider != null ? paymentProvider.trim().toLowerCase() : "asaas";
    }

    /**
     * E-mail do usuário de teste (tipo Comprador) para assinaturas em sandbox.
     * Quando definido e o ambiente for teste, este e-mail é usado como payer_email
     * para que collector e payer sejam ambos usuários de teste.
     *
     * @return e-mail configurado ou vazio
     */
    public String getTestPayerEmail() {
        return testPayerEmail;
    }
    
    /**
     * Inicializa a configuração do Mercado Pago
     * 
     * Em produção, valida que variáveis obrigatórias estão configuradas.
     * A aplicação não deve iniciar se essas variáveis não estiverem configuradas em produção.
     */
    @PostConstruct
    public void init() {
        try {
            log.info("=== Iniciando configuração do Mercado Pago ===");
            log.info("Ambiente configurado: {} ({})", environment, isProduction() ? "PRODUÇÃO" : "TESTE / SANDBOX");
            if (!isProduction() && testPayerEmail != null && !testPayerEmail.isEmpty()) {
                log.info("E-mail de pagador de teste (sandbox): {} (será usado em preapproval)", maskEmail(testPayerEmail));
            }
            
            // Validação obrigatória em produção apenas quando Mercado Pago é o provedor ativo
            if (isProduction() && "mercadopago".equals(paymentProvider)) {
                validateProductionConfiguration();
            } else if (isProduction() && !"mercadopago".equals(paymentProvider)) {
                log.info("Provedor de pagamento ativo: {} — validação Mercado Pago em produção ignorada", paymentProvider);
            }
            
            // Log dos valores brutos (mascarados) para debug
            log.debug("Test Access Token (raw): {}", testAccessToken != null && !testAccessToken.isEmpty() ? maskKey(testAccessToken) : "[VAZIO]");
            log.debug("Prod Access Token (raw): {}", prodAccessToken != null && !prodAccessToken.isEmpty() ? maskKey(prodAccessToken) : "[VAZIO]");
            
            String accessToken = getAccessToken();
            
            log.info("Access Token selecionado: {}", accessToken != null && !accessToken.trim().isEmpty() ? maskKey(accessToken) : "[VAZIO]");
            
            if (isValidAccessToken(accessToken)) {
                com.mercadopago.MercadoPagoConfig.setAccessToken(accessToken);
                log.info("✓ Mercado Pago configurado com sucesso!");
                if (!isProduction()) {
                    log.info("  Lembrete: MERCADOPAGO_TEST_ACCESS_TOKEN deve ser o token da seção 'Credenciais de teste' do painel (não o de produção).");
                }
                log.info("  Ambiente: {}", environment);
                if (publicUrl != null && !publicUrl.isBlank()) {
                    log.info("  Public URL: {}", publicUrl);
                    log.info("  Webhook URL: {}", getWebhookUrl());
                } else {
                    log.warn("  Public URL não configurada. Defina APP_PUBLIC_URL para construir o webhook.");
                }
            log.info("  Success URL: {}", getSuccessUrl());
            log.info("  Cancel URL: {}", getCancelUrl());
            log.info("  Pending URL: {}", getPendingUrl());
            validateUrls();
            } else {
                log.warn("✗ Mercado Pago não configurado - Access Token inválido ou não definido");
            log.warn("Para habilitar Mercado Pago, defina as variáveis de ambiente:");
            log.warn("  - MERCADOPAGO_ENVIRONMENT=test (ou production)");
            log.warn("  - MERCADOPAGO_TEST_ACCESS_TOKEN (para ambiente de teste)");
            log.warn("  - MERCADOPAGO_PROD_ACCESS_TOKEN (para ambiente de produção)");
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                log.debug("Access Token recebido (mas inválido): {}", maskKey(accessToken));
            }
            }
            log.info("=== Configuração do Mercado Pago concluída ===");
        } catch (IllegalStateException e) {
            // Em produção, se configuração obrigatória falhar, não permitir iniciar
            log.error("❌ ERRO CRÍTICO: Configuração obrigatória não encontrada em produção!");
            log.error("A aplicação não pode iniciar sem as configurações obrigatórias.");
            throw e;
        } catch (Exception e) {
            log.error("Erro durante inicialização do Mercado Pago: {}", e.getMessage(), e);
            if (isProduction()) {
                // Em produção, erros críticos devem impedir inicialização
                throw new IllegalStateException("Erro crítico na configuração do Mercado Pago em produção", e);
            } else {
                log.warn("Aplicação continuará sem integração com Mercado Pago");
            }
        }
    }
    
    /**
     * Retorna a URL pública configurada (ngrok/domínio)
     */
    public String getPublicUrl() {
        return publicUrl;
    }
    
    /**
     * Constrói a URL completa do webhook
     */
    public String getWebhookUrl() {
        if (publicUrl == null || publicUrl.isBlank()) {
            return "";
        }
        String cleanPublicUrl = publicUrl.trim().replace("`", "").replace("'", "").replace("\"", "");
        String base = cleanPublicUrl.endsWith("/") ? cleanPublicUrl.substring(0, cleanPublicUrl.length() - 1) : cleanPublicUrl;
        return base + "/api/webhooks/mercadopago";
    }
    
    private void validateUrls() {
        validateUrlFormat(getSuccessUrl(), "mercadopago.success.url");
        validateUrlFormat(getCancelUrl(), "mercadopago.cancel.url");
        validateUrlFormat(getPendingUrl(), "mercadopago.pending.url");
        
        if (isProduction()) {
            enforceHttps(getSuccessUrl(), "mercadopago.success.url");
            enforceHttps(getCancelUrl(), "mercadopago.cancel.url");
            enforceHttps(getPendingUrl(), "mercadopago.pending.url");
        }
    }
    
    private void validateUrlFormat(String url, String propertyName) {
        if (url != null && url.contains(" ")) {
            throw new IllegalStateException("URL em " + propertyName + " não deve conter espaços: '" + url + "'");
        }
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("URL inválida em " + propertyName + ": " + url);
        }
    }
    
    private void enforceHttps(String url, String propertyName) {
        try {
            URL u = new URL(url);
            // Em ambiente de teste/sandbox, permitir HTTP ou HTTPS
            // O Mercado Pago pode rejeitar HTTP apenas em produção
            if (!isProduction()) {
                return;
            }
            if (!"https".equalsIgnoreCase(u.getProtocol())) {
                throw new IllegalStateException("Em produção, " + propertyName + " deve usar HTTPS: " + url);
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException("URL inválida em " + propertyName + ": " + url);
        }
    }
    
    /**
     * Valida configuração obrigatória em produção
     * 
     * Em produção, as seguintes variáveis são OBRIGATÓRIAS:
     * - MERCADOPAGO_PROD_ACCESS_TOKEN
     * - MERCADOPAGO_WEBHOOK_SECRET
     * 
     * Se alguma estiver ausente ou inválida, a aplicação não deve iniciar.
     */
    private void validateProductionConfiguration() {
        log.info("Validando configuração obrigatória para produção...");
        
        java.util.List<String> missingVars = new java.util.ArrayList<>();
        
        // Validar MERCADOPAGO_PROD_ACCESS_TOKEN
        if (prodAccessToken == null || prodAccessToken.trim().isEmpty() || !isValidAccessToken(prodAccessToken)) {
            missingVars.add("MERCADOPAGO_PROD_ACCESS_TOKEN");
        }
        
        // Validar MERCADOPAGO_WEBHOOK_SECRET
        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            missingVars.add("MERCADOPAGO_WEBHOOK_SECRET");
        }
        
        if (!missingVars.isEmpty()) {
            String errorMsg = String.format(
                "❌ ERRO CRÍTICO: Variáveis de ambiente obrigatórias não configuradas em produção:\n" +
                "  %s\n" +
                "\n" +
                "A aplicação NÃO pode iniciar sem essas configurações.\n" +
                "Configure as variáveis de ambiente antes de iniciar a aplicação em produção.",
                String.join("\n  ", missingVars)
            );
            log.error(errorMsg);
            throw new IllegalStateException("Configuração obrigatória ausente em produção: " + String.join(", ", missingVars));
        }
        
        log.info("✓ Configuração obrigatória validada com sucesso!");
        log.info("  - MERCADOPAGO_PROD_ACCESS_TOKEN: configurado");
        log.info("  - MERCADOPAGO_WEBHOOK_SECRET: configurado");
    }
    
    /**
     * Verifica se o Access Token é válido
     */
    private boolean isValidAccessToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        String trimmedToken = token.trim();
        // Access Token do Mercado Pago geralmente começa com TEST- ou APP_USR-
        return trimmedToken.startsWith("TEST-") || 
               trimmedToken.startsWith("APP_USR-") ||
               trimmedToken.length() > 20; // Validação básica de tamanho
    }
    
    /**
     * Mascara a chave para logs de segurança
     */
    private String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "[INVALID]"; 
        }
        return key.substring(0, 8) + "***" + key.substring(key.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email != null ? "***" : "";
        }
        int at = email.indexOf('@');
        return (at > 2 ? email.substring(0, 2) + "***" : "***") + email.substring(at);
    }
    
    /**
     * Verifica se o Mercado Pago está configurado
     */
    public boolean isMercadoPagoConfigured() {
        String accessToken = getAccessToken();
        return isValidAccessToken(accessToken);
    }
    
    /**
     * Retorna o Access Token baseado no ambiente
     */
    public String getAccessToken() {
        String token;
        String env;
        if ("production".equals(environment) || "prod".equals(environment)) {
            token = prodAccessToken != null ? prodAccessToken : "";
            env = "production";
        } else {
            token = testAccessToken != null ? testAccessToken : "";
            env = "test";
        }
        log.info("🔑 getAccessToken() - Ambiente: {}, Token vazio: {}, Token length: {}",
                env, token.isEmpty(), token.length());
        return token;
    }
    
    /**
     * Retorna a Public Key para uso no frontend (Checkout Transparente).
     * Usada apenas para inicializar o MercadoPago.js e gerar card_token.
     * Nunca expor o Access Token no frontend.
     */
    public String getPublicKey() {
        if (isProduction()) {
            return prodPublicKey != null ? prodPublicKey : "";
        }
        return testPublicKey != null ? testPublicKey : "";
    }

    /**
     * Retorna o ambiente atual
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Verifica se está em ambiente de produção
     */
    public boolean isProduction() {
        return "production".equals(environment) || "prod".equals(environment);
    }
    
    /**
     * Retorna o secret do webhook para validação
     * 
     * IMPORTANTE: Em produção, este secret é obrigatório.
     * Nunca deve ser logado ou exposto.
     */
    public String getWebhookSecret() {
        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            if (isProduction()) {
                throw new IllegalStateException("MERCADOPAGO_WEBHOOK_SECRET não configurado em produção");
            }
            return "";
        }
        return webhookSecret;
    }
    
    /**
     * Verifica se o webhook secret está configurado
     */
    public boolean isWebhookSecretConfigured() {
        return webhookSecret != null && !webhookSecret.trim().isEmpty();
    }
    
    /**
     * Retorna a URL de sucesso para redirecionamento
     */
    public String getSuccessUrl() {
        return computeUrl(successUrl, "/subscription/success");
    }
    
    /**
     * Retorna a URL de cancelamento para redirecionamento
     */
    public String getCancelUrl() {
        return computeUrl(cancelUrl, "/subscription/cancel");
    }
    
    /**
     * Retorna a URL de pendente para redirecionamento
     */
    public String getPendingUrl() {
        return computeUrl(pendingUrl, "/subscription/pending");
    }

    /**
     * URL base do checkout de assinaturas (link direto do plano).
     * Sempre usa www.mercadopago.com.br: o sandbox não expõe /subscriptions/checkout em outro host.
     * Modo teste/prod é definido pelas credenciais (TEST- vs APP_USR-) e pelo plano, não pela URL.
     * Pode ser sobrescrita com mercadopago.subscription.checkout.base.url.
     */
    public String getSubscriptionCheckoutBaseUrl() {
        if (subscriptionCheckoutBaseUrlOverride != null && !subscriptionCheckoutBaseUrlOverride.isBlank()) {
            return subscriptionCheckoutBaseUrlOverride.endsWith("/")
                    ? subscriptionCheckoutBaseUrlOverride.substring(0, subscriptionCheckoutBaseUrlOverride.length() - 1)
                    : subscriptionCheckoutBaseUrlOverride;
        }
        return "https://www.mercadopago.com.br";
    }

    private String computeUrl(String candidate, String suffix) {
        String url = null;
        if (candidate != null && !candidate.isBlank() && isUrl(candidate)) {
            url = candidate;
        } else if (publicUrl != null && !publicUrl.isBlank() && isUrl(publicUrl)) {
            String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            url = base + suffix;
        } else {
            String defaultBase = "https://gerenciamento-de-estoque-front.vercel.app";
            url = defaultBase + suffix;
        }
        
        // Sanitização final: remove espaços e crases que podem vir de configs mal formatadas
        if (url != null) {
            url = url.trim().replace("`", "").replace("'", "").replace("\"", "");
        }
        return url;
    }

    private boolean isUrl(String s) {
        try {
            new URL(s);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
