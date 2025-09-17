package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.StripeConfig;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import br.softsistem.Gerenciamento_de_estoque.service.SubscriptionService;
import br.softsistem.Gerenciamento_de_estoque.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
// Removido: import lombok.RequiredArgsConstructor;
import br.softsistem.Gerenciamento_de_estoque.dto.subscriptionDto.SubscriptionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para gerenciamento de assinaturas
 */
@RestController
@RequestMapping("/api/subscriptions")
// Removido: @RequiredArgsConstructor (Lombok)
@Tag(name = "Subscriptions", description = "Gerenciamento de assinaturas")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    private final UsuarioService usuarioService;
    private final PlanRepository planRepository;
    private final StripeConfig stripeConfig;

    // Construtor explícito para injeção via Spring (substitui Lombok @RequiredArgsConstructor)
    public SubscriptionController(
            SubscriptionService subscriptionService,
            UsuarioService usuarioService,
            PlanRepository planRepository,
            StripeConfig stripeConfig
    ) {
        this.subscriptionService = subscriptionService;
        this.usuarioService = usuarioService;
        this.planRepository = planRepository;
        this.stripeConfig = stripeConfig;
    }
    
    /**
     * Inicia um trial gratuito
     */
    @PostMapping("/trial")
    @Operation(summary = "Iniciar trial", description = "Inicia um período de teste gratuito de 14 dias")
    public ResponseEntity<?> startTrial(@RequestBody StartTrialRequest request, Authentication authentication) {
        try {
            Usuario user = usuarioService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));
            
            Subscription subscription = subscriptionService.createTrialSubscription(user, plan);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Trial iniciado com sucesso",
                    "subscriptionId", subscription.getId(),
                    "trialEnd", subscription.getTrialEnd()
            ));
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Cria sessão de checkout para upgrade de trial
     */
    @PostMapping("/{subscriptionId}/upgrade")
    @Operation(summary = "Upgrade trial", description = "Cria sessão de checkout para converter trial em assinatura paga")
    public ResponseEntity<?> upgradeSubscription(@PathVariable Long subscriptionId, Authentication authentication) {
        try {
            Usuario user = usuarioService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            
            String checkoutUrl = subscriptionService.createUpgradeCheckoutSession(subscriptionId);
            
            return ResponseEntity.ok(Map.of(
                    "checkoutUrl", checkoutUrl,
                    "message", "Sessão de checkout criada com sucesso"
            ));
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Cancela uma assinatura
     */
    @PostMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Cancelar assinatura", description = "Cancela uma assinatura ativa")
    public ResponseEntity<?> cancelSubscription(@PathVariable Long subscriptionId, Authentication authentication) {
        try {
            Usuario user = usuarioService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            
            subscriptionService.cancelSubscription(subscriptionId, user);
            
            return ResponseEntity.ok(Map.of("message", "Assinatura cancelada com sucesso"));
            
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Busca assinatura ativa do usuário
     */
    @GetMapping("/current")
    @Operation(summary = "Assinatura atual", description = "Retorna a assinatura ativa do usuário")
    public ResponseEntity<?> getCurrentSubscription(Authentication authentication) {
        try {
            Usuario user = usuarioService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            
            Optional<Subscription> subscription = subscriptionService.getActiveSubscription(user);
            
            if (subscription.isPresent()) {
                return ResponseEntity.ok(convertToSubscriptionDto(subscription.get()));
            } else {
                return ResponseEntity.ok(Map.of("message", "Nenhuma assinatura ativa encontrada"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Busca histórico de assinaturas do usuário
     */
    @GetMapping("/history")
    @Operation(summary = "Histórico de assinaturas", description = "Retorna o histórico de assinaturas do usuário")
    public ResponseEntity<?> getSubscriptionHistory(Authentication authentication) {
        try {
            Usuario user = usuarioService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            
            List<Subscription> subscriptions = subscriptionService.getUserSubscriptionHistory(user);
            List<SubscriptionDto> subscriptionDtos = subscriptions.stream()
                    .map(this::convertToSubscriptionDto)
                    .toList();
            
            return ResponseEntity.ok(subscriptionDtos);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Verifica acesso a uma funcionalidade
     */
    @GetMapping("/feature/{feature}")
    @Operation(summary = "Verificar acesso", description = "Verifica se o usuário tem acesso a uma funcionalidade específica")
    public ResponseEntity<?> checkFeatureAccess(@PathVariable String feature, Authentication authentication) {
        try {
            Usuario user = usuarioService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            
            boolean hasAccess = subscriptionService.hasFeatureAccess(user, feature);
            
            return ResponseEntity.ok(Map.of(
                    "feature", feature,
                    "hasAccess", hasAccess
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Retorna configuração pública do Stripe
     */
    @GetMapping("/stripe-config")
    @Operation(summary = "Configuração Stripe", description = "Retorna chave pública do Stripe para o frontend")
    public ResponseEntity<?> getStripeConfig() {
        return ResponseEntity.ok(Map.of(
                "publishableKey", stripeConfig.getPublishableKey()
        ));
    }
    
    /**
     * Página de sucesso após checkout
     */
    @GetMapping("/success")
    @Operation(summary = "Sucesso do checkout", description = "Página de redirecionamento após checkout bem-sucedido")
    public ResponseEntity<?> checkoutSuccess(@RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(Map.of(
                "message", "Checkout realizado com sucesso!",
                "sessionId", sessionId != null ? sessionId : "N/A"
        ));
    }
    
    /**
     * Página de cancelamento do checkout
     */
    @GetMapping("/cancel")
    @Operation(summary = "Cancelamento do checkout", description = "Página de redirecionamento após cancelamento do checkout")
    public ResponseEntity<?> checkoutCancel() {
        return ResponseEntity.ok(Map.of(
                "message", "Checkout cancelado. Você pode tentar novamente a qualquer momento."
        ));
    }
    
    /**
     * Converte Subscription para DTO
     */
    private SubscriptionDto convertToSubscriptionDto(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setPlanName(subscription.getPlan().getName());
        dto.setPlanType(subscription.getPlan().getType());
        dto.setStatus(subscription.getStatus());
        dto.setTrialStart(subscription.getTrialStart());
        dto.setTrialEnd(subscription.getTrialEnd());
        dto.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        dto.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        dto.setIsInTrial(subscription.isInTrial());
        dto.setIsTrialEndingSoon(subscription.isTrialEndingSoon());
        dto.setIsActive(subscription.isActive());
        dto.setCreatedAt(subscription.getCreatedAt());
        return dto;
    }
    
    // DTOs
    
    public static class StartTrialRequest {
        private Long planId;
        
        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }
    }
    
}