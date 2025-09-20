package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.config.StripeConfig;
import br.softsistem.Gerenciamento_de_estoque.dto.SubscriptionDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de assinaturas e integração com Stripe
 */
@Service
@Transactional
public class SubscriptionService {
    
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UsuarioRepository usuarioRepository;
    private final StripeConfig stripeConfig;
    
    @Autowired
    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            UsuarioRepository usuarioRepository,
            StripeConfig stripeConfig
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.usuarioRepository = usuarioRepository;
        this.stripeConfig = stripeConfig;
    }
    
    /**
     * Busca assinatura atual do usuário
     */
    public Optional<Subscription> getCurrentSubscription(Long userId) {
        return subscriptionRepository.findByUserIdAndStatusIn(
            userId, 
            List.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE)
        );
    }
    
    /**
     * Cria uma nova assinatura com trial de 14 dias
     */
    public Subscription createTrialSubscription(Long userId, Long planId) {
        log.info("Criando assinatura trial para usuário {} com plano {}", userId, planId);
        
        Usuario user = usuarioRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));
        
        // Verificar se usuário já tem assinatura ativa
        Optional<Subscription> existingSubscription = getCurrentSubscription(userId);
        if (existingSubscription.isPresent()) {
            throw new IllegalStateException("Usuário já possui uma assinatura ativa");
        }
        
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.TRIAL);
        subscription.setTrialStart(LocalDateTime.now());
        subscription.setTrialEnd(LocalDateTime.now().plusDays(14));
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(14));
        
        return subscriptionRepository.save(subscription);
    }
    
    /**
     * Cria sessão de checkout do Stripe
     */
    public String createCheckoutSession(Long userId, Long planId, String successUrl, String cancelUrl) throws StripeException {
        log.info("Criando sessão de checkout para usuário {} com plano {}", userId, planId);
        
        if (!stripeConfig.isStripeConfigured()) {
            throw new IllegalStateException("Stripe não está configurado");
        }
        
        Usuario user = usuarioRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));
        
        if (plan.getStripePriceId() == null || plan.getStripePriceId().isEmpty()) {
            throw new IllegalStateException("Plano não possui preço configurado no Stripe");
        }
        
        // Criar ou buscar cliente no Stripe
        String customerId = getOrCreateStripeCustomer(user);
        
        // Criar sessão de checkout
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setCustomer(customerId)
            .setSuccessUrl(successUrl != null ? successUrl : stripeConfig.getSuccessUrl())
            .setCancelUrl(cancelUrl != null ? cancelUrl : stripeConfig.getCancelUrl())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPrice(plan.getStripePriceId())
                    .setQuantity(1L)
                    .build()
            )
            .setSubscriptionData(
                SessionCreateParams.SubscriptionData.builder()
                    .setTrialPeriodDays(14L) // 14 dias de trial
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("plan_id", planId.toString())
                    .build()
            );
        
        Session session = Session.create(paramsBuilder.build());
        
        log.info("Sessão de checkout criada: {}", session.getId());
        return session.getUrl();
    }
    
    /**
     * Busca ou cria cliente no Stripe
     */
    private String getOrCreateStripeCustomer(Usuario user) throws StripeException {
        // Buscar assinatura existente com customer ID
        Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(user.getId());
        if (existingSubscription.isPresent() && existingSubscription.get().getStripeCustomerId() != null) {
            return existingSubscription.get().getStripeCustomerId();
        }
        
        // Criar novo cliente no Stripe
        CustomerCreateParams params = CustomerCreateParams.builder()
            .setEmail(user.getEmail())
            .setName(user.getUsername())
            .putMetadata("user_id", user.getId().toString())
            .build();
        
        Customer customer = Customer.create(params);
        log.info("Cliente Stripe criado: {} para usuário {}", customer.getId(), user.getId());
        
        return customer.getId();
    }
    
    /**
     * Atualiza status da assinatura baseado em evento do Stripe
     */
    public void updateSubscriptionFromStripe(String stripeSubscriptionId, SubscriptionStatus status, 
                                            LocalDateTime currentPeriodStart, LocalDateTime currentPeriodEnd) {
        log.info("Atualizando assinatura {} para status {}", stripeSubscriptionId, status);
        
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
        if (subscriptionOpt.isEmpty()) {
            log.warn("Assinatura não encontrada para Stripe ID: {}", stripeSubscriptionId);
            return;
        }
        
        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(status);
        subscription.setCurrentPeriodStart(currentPeriodStart);
        subscription.setCurrentPeriodEnd(currentPeriodEnd);
        
        if (status == SubscriptionStatus.CANCELED) {
            subscription.setCanceledAt(LocalDateTime.now());
        } else if (status == SubscriptionStatus.EXPIRED) {
            subscription.setEndedAt(LocalDateTime.now());
        }
        
        subscriptionRepository.save(subscription);
        log.info("Assinatura atualizada com sucesso");
    }
    
    /**
     * Cancela assinatura
     */
    public void cancelSubscription(Long userId) throws StripeException {
        log.info("Cancelando assinatura do usuário {}", userId);
        
        Optional<Subscription> subscriptionOpt = getCurrentSubscription(userId);
        if (subscriptionOpt.isEmpty()) {
            throw new IllegalStateException("Usuário não possui assinatura ativa");
        }
        
        Subscription subscription = subscriptionOpt.get();
        
        // Cancelar no Stripe se tiver ID
        if (subscription.getStripeSubscriptionId() != null && stripeConfig.isStripeConfigured()) {
            try {
                com.stripe.model.Subscription stripeSubscription = 
                    com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());
                stripeSubscription.cancel();
                log.info("Assinatura cancelada no Stripe: {}", subscription.getStripeSubscriptionId());
            } catch (StripeException e) {
                log.error("Erro ao cancelar assinatura no Stripe: {}", e.getMessage());
                throw e;
            }
        }
        
        // Atualizar status local
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
        
        log.info("Assinatura cancelada com sucesso");
    }
    
    /**
     * Verifica se usuário pode acessar funcionalidade baseado no plano
     */
    public boolean canUserAccess(Long userId, String feature) {
        Optional<Subscription> subscriptionOpt = getCurrentSubscription(userId);
        if (subscriptionOpt.isEmpty()) {
            return false; // Sem assinatura = sem acesso
        }
        
        Subscription subscription = subscriptionOpt.get();
        Plan plan = subscription.getPlan();
        
        // Verificar recursos específicos do plano
        return switch (feature.toLowerCase()) {
            case "reports" -> Boolean.TRUE.equals(plan.getHasReports());
            case "analytics" -> Boolean.TRUE.equals(plan.getHasAdvancedAnalytics());
            case "api_access" -> Boolean.TRUE.equals(plan.getHasApiAccess());
            default -> true; // Funcionalidades básicas sempre disponíveis
        };
    }
    
    /**
     * Verifica limites de uso baseado no plano
     */
    public boolean isWithinLimits(Long userId, String limitType, int currentCount) {
        Optional<Subscription> subscriptionOpt = getCurrentSubscription(userId);
        if (subscriptionOpt.isEmpty()) {
            return false;
        }
        
        Plan plan = subscriptionOpt.get().getPlan();
        
        return switch (limitType.toLowerCase()) {
            case "users" -> plan.getMaxUsers() == null || currentCount < plan.getMaxUsers();
            case "products" -> plan.getMaxProducts() == null || currentCount < plan.getMaxProducts();
            case "organizations" -> plan.getMaxOrganizations() == null || currentCount < plan.getMaxOrganizations();
            default -> true;
        };
    }
    
    /**
     * Lista todas as assinaturas de um usuário
     */
    public List<Subscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Cria portal do cliente Stripe
     */
    public String createCustomerPortalSession(Long userId, String returnUrl) throws StripeException {
        if (!stripeConfig.isStripeConfigured()) {
            throw new IllegalStateException("Stripe não está configurado");
        }
        
        Optional<Subscription> subscriptionOpt = getCurrentSubscription(userId);
        if (subscriptionOpt.isEmpty()) {
            throw new IllegalStateException("Usuário não possui assinatura ativa");
        }
        
        String customerId = subscriptionOpt.get().getStripeCustomerId();
        if (customerId == null) {
            throw new IllegalStateException("Cliente não possui ID do Stripe");
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("return_url", returnUrl != null ? returnUrl : stripeConfig.getSuccessUrl());
        
        com.stripe.model.billingportal.Session portalSession = 
            com.stripe.model.billingportal.Session.create(params);
        
        return portalSession.getUrl();
    }
    
    /**
     * Processa criação de assinatura via webhook
     */
    public void processSubscriptionCreated(String stripeSubscriptionId, String customerId, 
                                         String priceId, LocalDateTime currentPeriodStart, 
                                         LocalDateTime currentPeriodEnd, boolean isTrialing) {
        log.info("Processando criação de assinatura: {}", stripeSubscriptionId);
        
        try {
            // Buscar plano pelo price ID
            Optional<Plan> planOpt = planRepository.findByStripePriceId(priceId);
            if (planOpt.isEmpty()) {
                log.error("Plano não encontrado para price ID: {}", priceId);
                return;
            }
            
            // Buscar usuário pelo customer ID
        Optional<Subscription> existingSubscription = subscriptionRepository.findFirstByStripeCustomerId(customerId);
        if (existingSubscription.isEmpty()) {
            log.error("Usuário não encontrado para customer ID: {}", customerId);
            return;
        }
            
            Subscription subscription = existingSubscription.get();
            subscription.setStripeSubscriptionId(stripeSubscriptionId);
            subscription.setStatus(isTrialing ? SubscriptionStatus.TRIAL : SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodStart(currentPeriodStart);
            subscription.setCurrentPeriodEnd(currentPeriodEnd);
            subscription.setPlan(planOpt.get());
            
            subscriptionRepository.save(subscription);
            log.info("Assinatura criada/atualizada com sucesso");
            
        } catch (Exception e) {
            log.error("Erro ao processar criação de assinatura: {}", e.getMessage(), e);
        }
    }

    // Métodos para encapsular lógica do controller
    
    public SubscriptionDto getCurrentSubscriptionForUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        Optional<Subscription> subscription = getCurrentSubscription(userId);
        return subscription.map(this::convertToDto).orElse(null);
    }
    
    public Map<String, Object> createSubscriptionForUser(Long planId) throws StripeException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        // Verificar se já tem assinatura ativa
        Optional<Subscription> existingSubscription = getCurrentSubscription(userId);
        if (existingSubscription.isPresent() && 
            existingSubscription.get().getStatus() == SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Usuário já possui uma assinatura ativa");
        }
        
        String sessionId = createCheckoutSession(userId, planId, 
            "http://localhost:8080/subscription/success", 
            "http://localhost:8080/subscription/cancel");
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        return response;
    }
    
    public Map<String, String> cancelSubscriptionForUser() throws StripeException {
        Long userId = SecurityUtils.getCurrentUserId();
        cancelSubscription(userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Assinatura cancelada com sucesso");
        return response;
    }
    
    public Map<String, Object> getCustomerPortalForUser() throws StripeException {
        Long userId = SecurityUtils.getCurrentUserId();
        String portalUrl = createCustomerPortalSession(userId, "http://localhost:8080/subscription");
        
        Map<String, Object> response = new HashMap<>();
        response.put("url", portalUrl);
        return response;
    }
    
    public List<SubscriptionDto> getSubscriptionHistoryForUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Subscription> subscriptions = getUserSubscriptions(userId);
        return subscriptions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Map<String, Boolean> checkFeatureAccessForUser(String feature) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean hasAccess = canUserAccess(userId, feature);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("hasAccess", hasAccess);
        return response;
    }
    
    public Map<String, Object> checkUsageLimitsForUser(String limitType, int currentCount) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean withinLimits = isWithinLimits(userId, limitType, currentCount);
        
        Map<String, Object> response = new HashMap<>();
        response.put("withinLimits", withinLimits);
        response.put("currentCount", currentCount);
        return response;
    }
    
    public List<SubscriptionDto> getAllSubscriptionsForAdmin() {
        // TODO: Implementar verificação de autorização de admin
        // TODO: Filtrar assinaturas por organização quando necessário
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        return subscriptions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private SubscriptionDto convertToDto(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setUserId(subscription.getUser().getId());
        dto.setStatus(subscription.getStatus());
        dto.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        dto.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        dto.setCreatedAt(subscription.getCreatedAt());
        dto.setUpdatedAt(subscription.getUpdatedAt());
        
        if (subscription.getPlan() != null) {
            dto.setPlanName(subscription.getPlan().getName());
            dto.setPlanPrice(subscription.getPlan().getPrice());
        }
        
        return dto;
    }
    
    /**
     * Envia alertas para trials que estão próximos do fim
     */
    public void sendTrialEndingAlerts() {
        log.info("Verificando trials próximos do fim para envio de alertas");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);
        
        List<Subscription> trialsEndingSoon = subscriptionRepository.findTrialsEndingSoon(threeDaysFromNow);
        
        for (Subscription subscription : trialsEndingSoon) {
            try {
                log.info("Enviando alerta de fim de trial para usuário: {}", subscription.getUser().getEmail());
                
                // Aqui você pode implementar o envio de email ou notificação
                // Por exemplo: emailService.sendTrialEndingAlert(subscription);
                
                // Marcar como alerta enviado
                subscription.setTrialWarningSent(true);
                subscriptionRepository.save(subscription);
                
                log.info("Alerta de fim de trial enviado para: {}", subscription.getUser().getEmail());
            } catch (Exception e) {
                log.error("Erro ao enviar alerta de fim de trial para usuário {}: {}", 
                    subscription.getUser().getEmail(), e.getMessage(), e);
            }
        }
        
        log.info("Processamento de alertas de fim de trial concluído. {} alertas enviados", trialsEndingSoon.size());
    }
    
    /**
     * Processa trials expirados, alterando status para EXPIRED
     */
    public void processExpiredTrials() {
        log.info("Processando trials expirados");
        
        LocalDateTime now = LocalDateTime.now();
        
        List<Subscription> expiredTrials = subscriptionRepository.findExpiredTrials(now);
        
        for (Subscription subscription : expiredTrials) {
            try {
                log.info("Processando trial expirado para usuário: {}", subscription.getUser().getEmail());
                
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                subscription.setEndedAt(now);
                subscriptionRepository.save(subscription);
                
                log.info("Trial expirado processado para usuário: {}", subscription.getUser().getEmail());
            } catch (Exception e) {
                log.error("Erro ao processar trial expirado para usuário {}: {}", 
                    subscription.getUser().getEmail(), e.getMessage(), e);
            }
        }
        
        log.info("Processamento de trials expirados concluído. {} trials processados", expiredTrials.size());
    }
}