package br.softsistem.Gerenciamento_de_estoque.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Stripe para integração de pagamentos
 */
@Configuration
public class StripeConfig {
    
    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);
    
    private final String stripeApiKey;
    private final String stripePublishableKey;
    private final String stripeWebhookSecret;
    private final String stripeSuccessUrl;
    private final String stripeCancelUrl;
    
    /**
     * Construtor com injeção de dependências
     */
    public StripeConfig(
            @Value("${stripe.api.key:}") String stripeApiKey,
            @Value("${stripe.publishable.key:}") String stripePublishableKey,
            @Value("${stripe.webhook.secret:}") String stripeWebhookSecret,
            @Value("${stripe.success.url:http://localhost:8080/subscription/success}") String stripeSuccessUrl,
            @Value("${stripe.cancel.url:http://localhost:8080/subscription/cancel}") String stripeCancelUrl
    ) {
        this.stripeApiKey = stripeApiKey;
        this.stripePublishableKey = stripePublishableKey;
        this.stripeWebhookSecret = stripeWebhookSecret;
        this.stripeSuccessUrl = stripeSuccessUrl;
        this.stripeCancelUrl = stripeCancelUrl;
    }
    
    /**
     * Inicializa a configuração do Stripe
     */
    @PostConstruct
    public void init() {
        try {
            log.debug("Iniciando configuração do Stripe...");
            log.debug("Stripe API Key presente: {}", stripeApiKey != null && !stripeApiKey.trim().isEmpty());
            log.debug("Stripe Publishable Key presente: {}", stripePublishableKey != null && !stripePublishableKey.trim().isEmpty());
            
            if (isValidStripeKey(stripeApiKey)) {
                Stripe.apiKey = stripeApiKey;
                log.info("Stripe configurado com sucesso");
                log.debug("Stripe Success URL: {}", stripeSuccessUrl);
                log.debug("Stripe Cancel URL: {}", stripeCancelUrl);
            } else {
                log.warn("Stripe não configurado - chaves de API inválidas ou não definidas");
                log.info("Para habilitar Stripe, defina chaves válidas: STRIPE_SECRET_KEY, STRIPE_PUBLISHABLE_KEY, STRIPE_WEBHOOK_SECRET");
                if (stripeApiKey != null) {
                    log.debug("Chave API atual: {}", maskKey(stripeApiKey));
                }
            }
        } catch (Exception e) {
            log.error("Erro durante inicialização do Stripe: {}", e.getMessage(), e);
            log.warn("Aplicação continuará sem integração com Stripe");
        }
    }
    
    /**
     * Verifica se a chave do Stripe é válida (não é placeholder ou vazia)
     */
    private boolean isValidStripeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        // Verifica se não é um placeholder de teste
        String trimmedKey = key.trim();
        return !trimmedKey.startsWith("sk_test_51234567890") && 
               !trimmedKey.startsWith("pk_test_51234567890") &&
               (trimmedKey.startsWith("sk_") || trimmedKey.startsWith("pk_"));
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
    
    /**
     * Verifica se o Stripe está configurado
     */
    public boolean isStripeConfigured() {
        return isValidStripeKey(stripeApiKey) && isValidStripeKey(stripePublishableKey);
    }
    
    /**
     * Retorna a chave pública do Stripe para uso no frontend
     */
    public String getPublishableKey() {
        return stripePublishableKey != null ? stripePublishableKey : "";
    }
    
    /**
     * Retorna o secret do webhook para validação
     */
    public String getWebhookSecret() {
        return stripeWebhookSecret != null ? stripeWebhookSecret : "";
    }
    
    /**
     * Retorna a URL de sucesso para redirecionamento
     */
    public String getSuccessUrl() {
        return stripeSuccessUrl;
    }
    
    /**
     * Retorna a URL de cancelamento para redirecionamento
     */
    public String getCancelUrl() {
        return stripeCancelUrl;
    }
}