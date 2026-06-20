package br.softsistem.Gerenciamento_de_estoque.config;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.service.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class SubscriptionAccessFilter extends OncePerRequestFilter {

    private final SubscriptionService subscriptionService;
    private static final Logger log = LoggerFactory.getLogger(SubscriptionAccessFilter.class);

    public SubscriptionAccessFilter(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        String method = request.getMethod();

        if (isAllowed(method, path)) {
            chain.doFilter(request, response);
            return;
        }

        Usuario usuario = null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Usuario u) {
            usuario = u;
        }

        if (usuario != null) {
            if (usuario.hasSubscriptionBypass()) {
                String correlationId = request.getHeader("X-Correlation-Id");
                if (correlationId == null || correlationId.isBlank()) {
                    correlationId = request.getHeader("X-Request-Id");
                }
                String userAgent = request.getHeader("User-Agent");
                String ip = request.getRemoteAddr();
                Long orgId = SecurityUtils.getCurrentOrgId();
                log.warn("[SECURITY] Subscription bypass used userId={} orgId={} method={} path={} ip={} userAgent={} correlationId={}",
                        usuario.getId(), orgId, method, path, ip, userAgent, correlationId);
                chain.doFilter(request, response);
                return;
            }
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            chain.doFilter(request, response);
            return;
        }

        Optional<Subscription> current = subscriptionService.getCurrentSubscription(userId);
        if (current.isPresent() && Boolean.TRUE.equals(current.get().getAccessBlocked())) {
            writeJson(response, 423, "ACCESS_LOCKED", "CHARGEBACK", "CONTACT_SUPPORT");
            return;
        }
        if (current.isEmpty()) {
            writeJson(response, 402, "SUBSCRIPTION_REQUIRED", "NO_SUBSCRIPTION", "REGISTER_OR_CONTACT");
            return;
        }

        Subscription sub = current.get();
        LocalDateTime now = LocalDateTime.now();
        if (sub.getStatus() == SubscriptionStatus.TRIAL) {
            if (sub.getTrialEnd() == null || !now.isBefore(sub.getTrialEnd())) {
                writeJson(response, 402, "SUBSCRIPTION_REQUIRED", "TRIAL_ENDED", "CHOOSE_PLAN");
                return;
            }
        } else if (sub.getStatus() == SubscriptionStatus.INCOMPLETE) {
            if (sub.getAsaasPaymentId() == null || sub.getAsaasPaymentId().isBlank()) {
                writeJson(response, 402, "SUBSCRIPTION_REQUIRED", "NO_SUBSCRIPTION", "CHOOSE_PLAN");
                return;
            }
        } else if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
            if (sub.getCurrentPeriodEnd() == null || !now.isBefore(sub.getCurrentPeriodEnd())) {
                writeJson(response, 402, "SUBSCRIPTION_REQUIRED", "EXPIRED", "CHOOSE_PLAN");
                return;
            }
        } else {
            // CANCELED, PAST_DUE, etc.: webhook "cancelled" do Mercado Pago atualiza status → usuário bloqueado
            writeJson(response, 402, "SUBSCRIPTION_REQUIRED", "CANCELED", "CHOOSE_PLAN");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAllowed(String method, String path) {
        if (path == null) return false;
        if (path.startsWith("/auth/login") || path.startsWith("/auth/register")) return true;
        if (path.startsWith("/api/plans")) return true;
        if (path.startsWith("/api/asaas/config")) return true;
        if (path.startsWith("/webhooks/mercadopago") || path.startsWith("/api/webhooks/mercadopago")) return true;
        if (path.startsWith("/webhooks/asaas") || path.startsWith("/api/webhooks/asaas")) return true;
        if (path.startsWith("/api/subscription/current")) return true;
        if (path.startsWith("/api/subscription/create")) return true;
        if (path.startsWith("/api/subscription/checkout")) return true;
        if (path.startsWith("/api/subscription/sync-payment")) return true;
        if (path.startsWith("/api/subscription/simulate-payment")) return true;
        if (path.startsWith("/api/subscription/history")) return true;
        // Checkout Transparente: frontend precisa da Public Key para tokenizar o cartão (usuário ainda não tem plano)
        if (path.startsWith("/api/mercadopago/public-key")) return true;
        if (path.startsWith("/api/subscription/customer-portal")) return true;
        if (path.startsWith("/api/subscription/cancel")) return true;
        if (path.startsWith("/api/pesquisa-preco")) return true;
        return false;
    }

    private void writeJson(HttpServletResponse response, int status, String code, String reason, String action) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"code\":\"" + code + "\",\"reason\":\"" + reason + "\",\"action\":\"" + action + "\"}";
        response.getWriter().write(body);
    }
}
