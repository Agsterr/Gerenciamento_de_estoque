package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache em memória quando Redis não está habilitado (produção Docker / local).
 */
@Configuration
@EnableCaching
@ConditionalOnMissingBean(CacheManager.class)
public class SimpleCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "produtos",
                "categorias",
                "usuarios",
                "entregas-relatorios",
                "organizacoes",
                "consumidores"
        );
    }
}
