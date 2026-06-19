package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

/**
 * Configuração para processamento de webhooks
 * 
 * Inclui:
 * - Timeout controlado
 * - Executor assíncrono configurado
 * - Pool de threads otimizado
 */
@Configuration
@EnableAsync
public class WebhookConfig {
    
    @Value("${webhook.processing.timeout.seconds:30}")
    private int processingTimeoutSeconds;
    
    @Value("${webhook.executor.core-pool-size:5}")
    private int corePoolSize;
    
    @Value("${webhook.executor.max-pool-size:10}")
    private int maxPoolSize;
    
    @Value("${webhook.executor.queue-capacity:100}")
    private int queueCapacity;
    
    /**
     * Executor assíncrono para processamento de webhooks
     * Configurado com timeout e pool de threads otimizado
     */
    @Bean(name = "webhookTaskExecutor")
    public java.util.concurrent.Executor webhookTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("webhook-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    /**
     * Retorna o timeout de processamento em segundos
     */
    public int getProcessingTimeoutSeconds() {
        return processingTimeoutSeconds;
    }
    
    /**
     * Retorna o timeout de processamento em milissegundos
     */
    public long getProcessingTimeoutMillis() {
        return TimeUnit.SECONDS.toMillis(processingTimeoutSeconds);
    }
}







