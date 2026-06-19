package br.softsistem.Gerenciamento_de_estoque.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;

/**
 * Gerencia o período de teste gratuito (15 dias) controlado pela aplicação.
 */
@Service
@Transactional
public class TrialSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TrialSubscriptionService.class);

    @Value("${app.subscription.trial-days:15}")
    private int trialDays;

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public TrialSubscriptionService(SubscriptionRepository subscriptionRepository, PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    public int getTrialDays() {
        return trialDays;
    }

    /**
     * Inicia trial de 15 dias para novo usuário (lógica interna, sem Asaas).
     */
    public Subscription startTrialForUser(Usuario user) {
        if (subscriptionRepository.hasActiveOrTrialSubscription(user)) {
            log.debug("Usuário {} já possui assinatura/trial", user.getId());
            return subscriptionRepository.findByUserId(user.getId()).orElse(null);
        }

        Plan plan = resolveDefaultPlan();

        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.TRIAL);
        subscription.setPaymentProvider("ASAAS");
        subscription.setTrialStart(now);
        subscription.setTrialEnd(now.plusDays(trialDays));
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plusDays(trialDays));
        subscription.setAccessBlocked(false);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Trial de {} dias iniciado para user={} até {}", trialDays, user.getId(), saved.getTrialEnd());
        return saved;
    }

    private Plan resolveDefaultPlan() {
        Optional<Plan> basic = planRepository.findByType(PlanType.BASIC);
        if (basic.isPresent() && Boolean.TRUE.equals(basic.get().getIsActive())) {
            return basic.get();
        }
        return planRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum plano ativo encontrado para iniciar o trial"));
    }
}
