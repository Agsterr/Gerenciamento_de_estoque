package br.softsistem.Gerenciamento_de_estoque.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Reduz abuso do cadastro público (contas falsas para repetir o trial gratuito):
 * limite de cadastros por IP em 24h, bloqueio de e-mails descartáveis e de aliases "+tag".
 */
@Component
public class RegistrationAbuseGuard {

    private static final Logger log = LoggerFactory.getLogger(RegistrationAbuseGuard.class);
    private static final Duration WINDOW = Duration.ofHours(24);

    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            "mailinator.com", "guerrillamail.com", "guerrillamail.net", "10minutemail.com", "10minutemail.net",
            "tempmail.com", "temp-mail.org", "temp-mail.io", "tempmailo.com", "yopmail.com", "yopmail.fr",
            "trashmail.com", "trashmail.net", "getnada.com", "nada.email", "sharklasers.com", "grr.la",
            "dispostable.com", "maildrop.cc", "throwawaymail.com", "fakeinbox.com", "mailnesia.com",
            "mintemail.com", "emailondeck.com", "moakt.com", "mohmal.com", "burnermail.io", "spam4.me",
            "tmpmail.org", "tmpmail.net", "discard.email", "mail.tm", "inboxkitten.com", "mytemp.email");

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();

    @Value("${app.registration.max-per-ip-per-day:3}")
    private int maxPerIpPerDay;

    /** Valida o cadastro; lança IllegalArgumentException (HTTP 400) quando deve ser recusado. */
    public void check(HttpServletRequest request, String email) {
        validateEmail(email);
        String ip = clientIp(request);
        if (!tryAcquire(ip)) {
            log.warn("Cadastro bloqueado: limite diário por IP excedido ip={}", ip);
            throw new IllegalArgumentException(
                    "Muitos cadastros a partir desta rede nas últimas 24 horas. Tente novamente amanhã ou fale com o suporte.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int at = normalized.lastIndexOf('@');
        if (at < 1) {
            return;
        }
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at + 1);
        if (local.contains("+")) {
            throw new IllegalArgumentException("Use o e-mail principal, sem sufixo \"+\" (ex.: nome@dominio.com).");
        }
        if (DISPOSABLE_DOMAINS.contains(domain)) {
            throw new IllegalArgumentException("E-mails temporários não são aceitos. Use um e-mail permanente.");
        }
    }

    private boolean tryAcquire(String ip) {
        Instant now = Instant.now();
        Deque<Instant> attempts = attemptsByIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(now.minus(WINDOW))) {
                attempts.pollFirst();
            }
            if (attempts.size() >= maxPerIpPerDay) {
                return false;
            }
            attempts.addLast(now);
            return true;
        }
    }

    /** Remove IPs sem tentativas recentes para não crescer indefinidamente. */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 3_600_000L)
    void cleanup() {
        Instant limit = Instant.now().minus(WINDOW);
        attemptsByIp.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || e.getValue().peekLast().isBefore(limit);
            }
        });
    }

    private static String clientIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
