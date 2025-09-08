package br.softsistem.Gerenciamento_de_estoque.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serviço para monitoramento e gestão do cache Redis
 * Fornece métricas e operações de limpeza para otimização
 * Só é ativado quando há um CacheManager disponível
 */
@Service
@ConditionalOnBean(CacheManager.class)
public class CacheMonitoringService {

    private final CacheManager cacheManager;

    public CacheMonitoringService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Retorna informações sobre todos os caches ativos
     */
    public Map<String, Object> getCacheInfo() {
        Map<String, Object> cacheInfo = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> info = new HashMap<>();
                info.put("name", cacheName);
                info.put("nativeCache", cache.getNativeCache().getClass().getSimpleName());
                cacheInfo.put(cacheName, info);
            }
        });
        
        return cacheInfo;
    }

    /**
     * Limpa um cache específico
     */
    public boolean clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return true;
        }
        return false;
    }

    /**
     * Limpa todos os caches
     */
    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    /**
     * Verifica se uma chave específica existe no cache
     */
    public boolean hasKey(String cacheName, String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            return cache.get(key) != null;
        }
        return false;
    }

    /**
     * Remove uma chave específica do cache
     */
    public boolean evictKey(String cacheName, String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            return true;
        }
        return false;
    }

    /**
     * Retorna estatísticas básicas dos caches
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCaches", cacheManager.getCacheNames().size());
        stats.put("cacheNames", cacheManager.getCacheNames());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
}