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
    private static final long WINDOW_SIZE_MILLIS = 60 * 1000; // 1 minuto
    
    // Armazenamento em memória para contadores (em produção, usar Redis)
    private final ConcurrentHashMap<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientId = getClientIdentifier(request);
        String endpoint = request.getRequestURI();
        
        // Aplicar rate limiting mais restritivo para endpoints de assinatura
        int maxRequests = isSubscriptionEndpoint(endpoint) ? 
            MAX_SUBSCRIPTION_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;
        
        if (isRateLimited(clientId, maxRequests)) {
            log.warn("Rate limit excedido para cliente: {} no endpoint: {}", clientId, endpoint);
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
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String clientIp = xForwardedFor != null ? xForwardedFor.split(",")[0] : request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        
        // Combinar IP e User-Agent para identificação mais precisa
        return clientIp + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }
    
    /**
     * Verifica se o endpoint é relacionado a assinaturas
     */
    private boolean isSubscriptionEndpoint(String endpoint) {
        return endpoint.startsWith("/api/subscriptions/") || 
               endpoint.contains("/create") || 
               endpoint.contains("/cancel") ||
               endpoint.contains("/portal");
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