package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.SubscriptionDto;
import br.softsistem.Gerenciamento_de_estoque.service.SubscriptionService;
import com.stripe.exception.StripeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller para gerenciar assinaturas de usuários
 */
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;
    
    /**
     * Obtém a assinatura atual do usuário
     */
    @GetMapping("/current")
    public ResponseEntity<SubscriptionDto> getCurrentSubscription() {
        SubscriptionDto subscription = subscriptionService.getCurrentSubscriptionForUser();
        if (subscription != null) {
            return ResponseEntity.ok(subscription);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Cria uma nova assinatura
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSubscription(@RequestParam Long planId) throws StripeException {
        Map<String, Object> response = subscriptionService.createSubscriptionForUser(planId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancela a assinatura do usuário
     */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelSubscription() throws StripeException {
        Map<String, String> response = subscriptionService.cancelSubscriptionForUser();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtém URL do portal do cliente Stripe
     */
    @GetMapping("/customer-portal")
    public ResponseEntity<Map<String, Object>> getCustomerPortal() throws StripeException {
        Map<String, Object> response = subscriptionService.getCustomerPortalForUser();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtém histórico de assinaturas do usuário
     */
    @GetMapping("/history")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionHistory() {
        List<SubscriptionDto> subscriptions = subscriptionService.getSubscriptionHistoryForUser();
        return ResponseEntity.ok(subscriptions);
    }
    
    /**
     * Verifica se o usuário tem acesso a uma funcionalidade
     */
    @GetMapping("/feature-access")
    public ResponseEntity<Map<String, Boolean>> checkFeatureAccess(@RequestParam String feature) {
        Map<String, Boolean> response = subscriptionService.checkFeatureAccessForUser(feature);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verifica limites de uso
     */
    @GetMapping("/usage-limits")
    public ResponseEntity<Map<String, Object>> checkUsageLimits(
            @RequestParam String limitType, 
            @RequestParam int currentCount) {
        Map<String, Object> response = subscriptionService.checkUsageLimitsForUser(limitType, currentCount);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lista todas as assinaturas (admin)
     */
    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionDto>> getAllSubscriptions() {
        List<SubscriptionDto> subscriptions = subscriptionService.getAllSubscriptionsForAdmin();
        return ResponseEntity.ok(subscriptions);
    }
}