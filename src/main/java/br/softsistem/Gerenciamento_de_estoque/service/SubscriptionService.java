package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciamento de assinaturas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final StripeService stripeService;
    private final EmailService emailService;
    
    @Value("${app.subscription.trial-days:14}")
    private int trialDays;
    
    /**
     * Cria uma nova assinatura em trial
     */
    @Transactional
    public Subscription createTrialSubscription(Usuario user, Plan plan) {
        // Verifica se o usuário já tem uma assinatura ativa
        Optional<Subscription> existingSubscription = subscriptionRepository.findActiveSubscriptionByUser(user);
        if (existingSubscription.isPresent()) {
            throw new IllegalStateException("Usuário já possui uma assinatura ativa");
        }
        
        try {
            // Cria cliente no Stripe se não existir
            Customer customer = stripeService.createCustomer(user);
            
            // Cria sessão de checkout para trial
            Session checkoutSession = stripeService.createTrialCheckoutSession(user, plan, customer.getId());
            
            // Cria assinatura local
            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setPlan(plan);
            subscription.setStatus(SubscriptionStatus.TRIAL);
            subscription.setStripeCustomerId(customer.getId());
            subscription.setTrialStart(LocalDateTime.now());
            subscription.setTrialEnd(LocalDateTime.now().plusDays(trialDays));
            subscription.setCurrentPeriodStart(LocalDateTime.now());
            subscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(trialDays));
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            
            // Envia email de boas-vindas
            emailService.sendTrialWelcomeEmail(user, plan, trialDays);
            
            log.info("Assinatura trial criada para usuário: {} no plano: {}", user.getId(), plan.getId());
            return savedSubscription;
            
        } catch (StripeException e) {
            log.error("Erro ao criar assinatura trial no Stripe para usuário: {}", user.getId(), e);
            throw new RuntimeException("Erro ao processar assinatura: " + e.getMessage());
        }
    }
    
    /**
     * Cria uma sessão de checkout para upgrade de trial para pago
     */
    public String createUpgradeCheckoutSession(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada"));
        
        if (subscription.getStatus() != SubscriptionStatus.TRIAL) {
            throw new IllegalStateException("Apenas assinaturas em trial podem ser atualizadas");
        }
        
        try {
            Session checkoutSession = stripeService.createPaymentCheckoutSession(
                subscription.getUser(), 
                subscription.getPlan(), 
                subscription.getStripeCustomerId()
            );
            
            return checkoutSession.getUrl();
            
        } catch (StripeException e) {
            log.error("Erro ao criar sessão de checkout para upgrade: {}", subscriptionId, e);
            throw new RuntimeException("Erro ao processar upgrade: " + e.getMessage());
        }
    }
    
    /**
     * Atualiza status da assinatura baseado no webhook do Stripe
     */
    @Transactional
    public void updateSubscriptionFromStripe(String stripeSubscriptionId, SubscriptionStatus newStatus) {
        Optional<Subscription> optionalSubscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
        
        if (optionalSubscription.isPresent()) {
            Subscription subscription = optionalSubscription.get();
            SubscriptionStatus oldStatus = subscription.getStatus();
            subscription.setStatus(newStatus);
            
            // Atualiza timestamps baseado no status
            switch (newStatus) {
                case ACTIVE:
                    if (oldStatus == SubscriptionStatus.TRIAL) {
                        // Trial convertido para pago
                        emailService.sendTrialConvertedEmail(subscription.getUser(), subscription.getPlan());
                    }
                    break;
                case CANCELED:
                    subscription.setCanceledAt(LocalDateTime.now());
                    emailService.sendSubscriptionCanceledEmail(subscription.getUser(), subscription.getPlan());
                    break;
                case EXPIRED:
                    subscription.setEndedAt(LocalDateTime.now());
                    break;
            }
            
            subscriptionRepository.save(subscription);
            log.info("Assinatura {} atualizada de {} para {}", stripeSubscriptionId, oldStatus, newStatus);
        }
    }
    
    /**
     * Cancela uma assinatura
     */
    @Transactional
    public void cancelSubscription(Long subscriptionId, Usuario user) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada"));
        
        // Verifica se o usuário é o dono da assinatura
        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Usuário não autorizado a cancelar esta assinatura");
        }
        
        try {
            // Cancela no Stripe se houver ID
            if (subscription.getStripeSubscriptionId() != null) {
                stripeService.cancelSubscription(subscription.getStripeSubscriptionId());
            }
            
            // Atualiza status local
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCanceledAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            // Envia email de confirmação
            emailService.sendSubscriptionCanceledEmail(user, subscription.getPlan());
            
            log.info("Assinatura {} cancelada pelo usuário {}", subscriptionId, user.getId());
            
        } catch (StripeException e) {
            log.error("Erro ao cancelar assinatura no Stripe: {}", subscriptionId, e);
            throw new RuntimeException("Erro ao cancelar assinatura: " + e.getMessage());
        }
    }
    
    /**
     * Busca assinatura ativa do usuário
     */
    public Optional<Subscription> getActiveSubscription(Usuario user) {
        return subscriptionRepository.findActiveSubscriptionByUser(user);
    }
    
    /**
     * Verifica se usuário tem acesso a uma funcionalidade
     */
    public boolean hasFeatureAccess(Usuario user, String feature) {
        Optional<Subscription> subscription = getActiveSubscription(user);
        
        if (subscription.isEmpty() || !subscription.get().isActive()) {
            return false;
        }
        
        Plan plan = subscription.get().getPlan();
        
        return switch (feature) {
            case "reports" -> plan.getHasReports();
            case "advanced_analytics" -> plan.getHasAdvancedAnalytics();
            case "api_access" -> plan.getHasApiAccess();
            default -> true; // Funcionalidades básicas sempre disponíveis
        };
    }
    
    /**
     * Busca trials que estão próximos do fim
     */
    public List<Subscription> getTrialsEndingSoon() {
        LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
        return subscriptionRepository.findTrialsEndingSoon(threeDaysFromNow);
    }
    
    /**
     * Busca trials expirados
     */
    public List<Subscription> getExpiredTrials() {
        return subscriptionRepository.findExpiredTrials(LocalDateTime.now());
    }
    
    /**
     * Processa trials expirados
     */
    @Transactional
    public void processExpiredTrials() {
        List<Subscription> expiredTrials = getExpiredTrials();
        
        for (Subscription subscription : expiredTrials) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setEndedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            // Envia email de trial expirado
            emailService.sendTrialExpiredEmail(subscription.getUser(), subscription.getPlan());
            
            log.info("Trial expirado processado para usuário: {}", subscription.getUser().getId());
        }
    }
    
    /**
     * Envia alertas para trials que estão acabando
     */
    @Transactional
    public void sendTrialEndingAlerts() {
        List<Subscription> trialsEndingSoon = getTrialsEndingSoon();
        
        for (Subscription subscription : trialsEndingSoon) {
            if (!subscription.getTrialWarningSent()) {
                emailService.sendTrialEndingEmail(subscription.getUser(), subscription.getPlan(), subscription.getTrialEnd());
                
                subscription.setTrialWarningSent(true);
                subscriptionRepository.save(subscription);
                
                log.info("Alerta de trial enviado para usuário: {}", subscription.getUser().getId());
            }
        }
    }
    
    /**
     * Busca histórico de assinaturas do usuário
     */
    public List<Subscription> getUserSubscriptionHistory(Usuario user) {
        return subscriptionRepository.findByUserOrderByCreatedAtDesc(user);
    }
}