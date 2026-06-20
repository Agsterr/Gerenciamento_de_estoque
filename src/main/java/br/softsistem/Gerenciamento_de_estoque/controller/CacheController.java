package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.service.CacheMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para monitoramento e gerenciamento de cache.
 * Funciona com ou sem CacheMonitoringService disponível.
 */
@RestController
@RequestMapping("/api/cache")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CacheController {

    private final Optional<CacheMonitoringService> cacheMonitoringService;

    public CacheController(@Autowired(required = false) CacheMonitoringService cacheMonitoringService) {
        this.cacheMonitoringService = Optional.ofNullable(cacheMonitoringService);
    }

    /**
     * Retorna informações sobre todos os caches
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        if (cacheMonitoringService.isPresent()) {
            Map<String, Object> info = new HashMap<>(cacheMonitoringService.get().getCacheInfo());
            info.put("backend", "memory");
            return ResponseEntity.ok(info);
        }
        return ResponseEntity.ok(Map.of(
                "backend", "none",
                "message", "Monitoramento de cache indisponível neste ambiente."
        ));
    }

    /**
     * Retorna estatísticas dos caches
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        if (cacheMonitoringService.isPresent()) {
            Map<String, Object> stats = new HashMap<>(cacheMonitoringService.get().getCacheStatistics());
            stats.put("backend", "memory");
            return ResponseEntity.ok(stats);
        }
        return ResponseEntity.ok(Map.of(
                "totalCaches", 0,
                "cacheNames", java.util.List.of(),
                "backend", "none",
                "message", "Monitoramento de cache indisponível neste ambiente."
        ));
    }

    /**
     * Limpa um cache específico
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<String> clearCache(@PathVariable String cacheName) {
        if (cacheMonitoringService.isPresent()) {
            boolean cleared = cacheMonitoringService.get().clearCache(cacheName);
            if (cleared) {
                return ResponseEntity.ok("Cache '" + cacheName + "' limpo com sucesso");
            } else {
                return ResponseEntity.notFound().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Limpa todos os caches
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> clearAllCaches() {
        if (cacheMonitoringService.isPresent()) {
            cacheMonitoringService.get().clearAllCaches();
            return ResponseEntity.ok("Todos os caches foram limpos com sucesso");
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Remove uma chave específica de um cache
     */
    @DeleteMapping("/{cacheName}/keys/{key}")
    public ResponseEntity<String> evictKey(
            @PathVariable String cacheName, 
            @PathVariable String key) {
        if (cacheMonitoringService.isPresent()) {
            boolean evicted = cacheMonitoringService.get().evictKey(cacheName, key);
            if (evicted) {
                return ResponseEntity.ok("Chave '" + key + "' removida do cache '" + cacheName + "'");
            } else {
                return ResponseEntity.notFound().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Verifica se uma chave existe em um cache
     */
    @GetMapping("/{cacheName}/keys/{key}")
    public ResponseEntity<Map<String, Object>> hasKey(
            @PathVariable String cacheName, 
            @PathVariable String key) {
        if (cacheMonitoringService.isPresent()) {
            boolean exists = cacheMonitoringService.get().hasKey(cacheName, key);
            return ResponseEntity.ok(Map.of(
                "cacheName", cacheName,
                "key", key,
                "exists", exists
            ));
        }
        return ResponseEntity.notFound().build();
    }
}