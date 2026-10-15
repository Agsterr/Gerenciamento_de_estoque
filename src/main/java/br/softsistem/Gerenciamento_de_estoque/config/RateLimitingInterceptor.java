package br.softsistem.Gerenciamento_de_estoque.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor para implementar rate limiting básico
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitingInterceptor.class);
    
    // Configurações de rate limiting
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_SUBSCRIPTION_REQUESTS_PER_MINUTE = 10;
    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_SIZE_MILLIS = 60 * 1000; // 1 minuto
    
    // Armazenamento em memória para contadores (em produção, usar Redis)
    private final ConcurrentHashMap<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String endpoint = request.getRequestURI();

        int maxRequests;
        String bucket;
        if (isAuthEndpoint(endpoint)) {
            // Login/cadastro: limite por IP (sem User-Agent) para dificultar força bruta
            maxRequests = MAX_AUTH_REQUESTS_PER_MINUTE;
            bucket = getClientIp(request) + ":auth";
        } else if (isSubscriptionWriteEndpoint(endpoint)) {
            maxRequests = MAX_SUBSCRIPTION_REQUESTS_PER_MINUTE;
            bucket = getClientIdentifier(request) + ":sub-write";
        } else {
            maxRequests = MAX_REQUESTS_PER_MINUTE;
            bucket = getClientIdentifier(request) + ":sub-read";
        }
        
        if (isRateLimited(bucket, maxRequests)) {
            log.warn("Rate limit excedido para cliente: {} no endpoint: {}", bucket, endpoint);
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Rate limit excedido. Tente novamente em alguns minutos.\", \"code\": \"RATE_LIMIT_EXCEEDED\"}"
            );
            return false;
        }
        
        return true;
    }
    
    /**
     * Identifica o cliente baseado no IP e user agent
     */
    private String getClientIdentifier(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Combinar IP e User-Agent para identificação mais precisa
        return clientIp + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }
    
    /**
     * IP real do cliente. Atrás do Cloudflare Tunnel o header CF-Connecting-IP é
     * definido pelo Cloudflare e não pode ser forjado pelo cliente.
     */
    private String getClientIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        return xForwardedFor != null ? xForwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }

    private boolean isAuthEndpoint(String endpoint) {
        return endpoint.startsWith("/auth/");
    }

    /**
     * Endpoints de escrita (checkout, create, cancel) usam limite mais restritivo (10/min).
     * Leituras (current-user-id, current, history, etc.) usam limite normal (60/min).
     */
    private boolean isSubscriptionWriteEndpoint(String endpoint) {
        return endpoint.contains("/checkout") || endpoint.contains("/cancel") ||
               (endpoint.contains("/create") && !endpoint.contains("current"));
    }
    
    /**
     * Verifica se o cliente excedeu o rate limit
     */
    private boolean isRateLimited(String clientId, int maxRequests) {
        long currentTime = System.currentTimeMillis();
        
        RequestCounter counter = requestCounters.computeIfAbsent(clientId, k -> new RequestCounter());
        
        synchronized (counter) {
            // Reset counter se a janela de tempo expirou
            if (currentTime - counter.windowStart > WINDOW_SIZE_MILLIS) {
                counter.count.set(0);
                counter.windowStart = currentTime;
            }
            
            // Incrementar contador
            int currentCount = counter.count.incrementAndGet();
            
            // Verificar se excedeu o limite
            return currentCount > maxRequests;
        }
    }
    
    /**
     * Limpa contadores antigos periodicamente (chamado por scheduler)
     */
    public void cleanupOldCounters() {
        long currentTime = System.currentTimeMillis();
        
        requestCounters.entrySet().removeIf(entry -> {
            RequestCounter counter = entry.getValue();
            return currentTime - counter.windowStart > WINDOW_SIZE_MILLIS * 2;
        });
        
        log.debug("Limpeza de contadores concluída. Contadores ativos: {}", requestCounters.size());
    }
    
    /**
     * Classe interna para armazenar contador de requisições
     */
    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
    }
}
