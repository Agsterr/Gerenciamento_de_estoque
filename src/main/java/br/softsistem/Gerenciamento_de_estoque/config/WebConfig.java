package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração web para interceptors e outras configurações
 */
@Configuration
@EnableScheduling
public class WebConfig implements WebMvcConfigurer {
    
    private final RateLimitingInterceptor rateLimitingInterceptor;
    
    @Autowired
    public WebConfig(RateLimitingInterceptor rateLimitingInterceptor) {
        this.rateLimitingInterceptor = rateLimitingInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Aplicar rate limiting apenas em endpoints específicos
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns(
                    "/api/subscriptions/**",
                    "/api/webhooks/**"
                )
                .excludePathPatterns(
                    "/api/webhooks/stripe/test", // Excluir endpoint de teste
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                );
    }
    
    /**
     * Scheduler para limpeza periódica de contadores antigos
     * Executa a cada 5 minutos
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    public void cleanupRateLimitCounters() {
        rateLimitingInterceptor.cleanupOldCounters();
    }
}