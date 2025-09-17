package br.softsistem.Gerenciamento_de_estoque.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Stripe para integração de pagamentos
 */
@Configuration
@Slf4j
public class StripeConfig {
    
    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;
    
    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;
    
    @Value("${stripe.success.url}")
    private String stripeSuccessUrl;
    
    @Value("${stripe.cancel.url}")
    private String stripeCancelUrl;
    
    /**
     * Inicializa a configuração do Stripe
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        log.info("Stripe configurado com sucesso");
        log.debug("Stripe Success URL: {}", stripeSuccessUrl);
        log.debug("Stripe Cancel URL: {}", stripeCancelUrl);
    }
    
    /**
     * Retorna a chave pública do Stripe para uso no frontend
     */
    public String getPublishableKey() {
        return stripePublishableKey;
    }
    
    /**
     * Retorna o secret do webhook para validação
     */
    public String getWebhookSecret() {
        return stripeWebhookSecret;
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