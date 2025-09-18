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
        if (stripeApiKey != null && !stripeApiKey.trim().isEmpty()) {
            Stripe.apiKey = stripeApiKey;
            log.info("Stripe configurado com sucesso");
            log.debug("Stripe Success URL: {}", stripeSuccessUrl);
            log.debug("Stripe Cancel URL: {}", stripeCancelUrl);
        } else {
            log.warn("Stripe não configurado - variáveis de ambiente não definidas");
            log.info("Para habilitar Stripe, defina: STRIPE_SECRET_KEY, STRIPE_PUBLISHABLE_KEY, STRIPE_WEBHOOK_SECRET");
        }
    }
    
    /**
     * Verifica se o Stripe está configurado
     */
    public boolean isStripeConfigured() {
        return stripeApiKey != null && !stripeApiKey.trim().isEmpty() &&
               stripePublishableKey != null && !stripePublishableKey.trim().isEmpty();
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