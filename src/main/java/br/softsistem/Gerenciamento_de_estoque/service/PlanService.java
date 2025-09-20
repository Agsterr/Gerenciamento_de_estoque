package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.StripeConfig;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service para gerenciamento de planos e integração com Stripe
 */
@Service
@Transactional
public class PlanService {
    
    private static final Logger log = LoggerFactory.getLogger(PlanService.class);
    
    private final PlanRepository planRepository;
    private final StripeConfig stripeConfig;
    
    public PlanService(PlanRepository planRepository, StripeConfig stripeConfig) {
        this.planRepository = planRepository;
        this.stripeConfig = stripeConfig;
    }
    
    /**
     * Lista todos os planos ativos
     */
    public List<Plan> findAllActivePlans() {
        return planRepository.findByIsActiveTrue();
    }
    
    /**
     * Busca plano por ID
     */
    public Optional<Plan> findById(Long id) {
        return planRepository.findById(id);
    }
    
    /**
     * Busca plano por tipo
     */
    public Optional<Plan> findByType(PlanType type) {
        return planRepository.findByType(type);
    }
    
    /**
     * Sincroniza todos os planos com o Stripe
     * Cria produtos e preços no Stripe para planos que ainda não possuem
     */
    public void syncAllPlansWithStripe() {
        if (!stripeConfig.isStripeConfigured()) {
            log.warn("Stripe não está configurado. Sincronização de planos ignorada.");
            return;
        }
        
        List<Plan> plans = planRepository.findByIsActiveTrue();
        log.info("Iniciando sincronização de {} planos com Stripe", plans.size());
        
        for (Plan plan : plans) {
            try {
                syncPlanWithStripe(plan);
                log.info("Plano '{}' sincronizado com sucesso", plan.getName());
            } catch (StripeException e) {
                log.error("Erro do Stripe ao sincronizar plano '{}': {}", plan.getName(), e.getMessage(), e);
            } catch (IllegalStateException e) {
                log.error("Estado inválido ao sincronizar plano '{}': {}", plan.getName(), e.getMessage(), e);
            } catch (RuntimeException e) {
                log.error("Erro inesperado ao sincronizar plano '{}': {}", plan.getName(), e.getMessage(), e);
            }
        }
        
        log.info("Sincronização de planos concluída");
    }
    
    /**
     * Sincroniza um plano específico com o Stripe
     */
    public void syncPlanWithStripe(Plan plan) throws StripeException {
        if (!stripeConfig.isStripeConfigured()) {
            throw new IllegalStateException("Stripe não está configurado");
        }
        
        // Criar produto no Stripe se não existir
        if (plan.getStripeProductId() == null || plan.getStripeProductId().isEmpty()) {
            Product stripeProduct = createStripeProduct(plan);
            plan.setStripeProductId(stripeProduct.getId());
        }
        
        // Criar preço no Stripe se não existir
        if (plan.getStripePriceId() == null || plan.getStripePriceId().isEmpty()) {
            Price stripePrice = createStripePrice(plan);
            plan.setStripePriceId(stripePrice.getId());
        }
        
        planRepository.save(plan);
    }
    
    /**
     * Cria um produto no Stripe
     */
    private Product createStripeProduct(Plan plan) throws StripeException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("plan_id", plan.getId().toString());
        metadata.put("plan_type", plan.getType().name());
        metadata.put("max_users", plan.getMaxUsers() != null ? plan.getMaxUsers().toString() : "unlimited");
        metadata.put("max_products", plan.getMaxProducts() != null ? plan.getMaxProducts().toString() : "unlimited");
        metadata.put("max_organizations", plan.getMaxOrganizations() != null ? plan.getMaxOrganizations().toString() : "unlimited");
        
        ProductCreateParams params = ProductCreateParams.builder()
                .setName(plan.getName())
                .setDescription(plan.getDescription())
                .putAllMetadata(metadata)
                .build();
        
        Product product = Product.create(params);
        log.info("Produto Stripe criado: {} para plano: {}", product.getId(), plan.getName());
        
        return product;
    }
    
    /**
     * Cria um preço no Stripe
     */
    private Price createStripePrice(Plan plan) throws StripeException {
        // Converter preço para centavos (Stripe trabalha com centavos)
        long unitAmountCents = plan.getPrice().multiply(new BigDecimal("100")).longValue();
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("plan_id", plan.getId().toString());
        metadata.put("plan_type", plan.getType().name());
        
        PriceCreateParams params = PriceCreateParams.builder()
                .setProduct(plan.getStripeProductId())
                .setUnitAmount(unitAmountCents)
                .setCurrency("brl")
                .setRecurring(
                    PriceCreateParams.Recurring.builder()
                        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                        .build()
                )
                .putAllMetadata(metadata)
                .build();
        
        Price price = Price.create(params);
        log.info("Preço Stripe criado: {} para plano: {} (R$ {})", price.getId(), plan.getName(), plan.getPrice());
        
        return price;
    }
    
    /**
     * Valida se um usuário pode ser adicionado ao plano
     */
    public boolean canAddUser(Plan plan, int currentUserCount) {
        if (plan.getMaxUsers() == null) {
            return true; // Ilimitado
        }
        return currentUserCount < plan.getMaxUsers();
    }
    
    /**
     * Valida se um produto pode ser adicionado ao plano
     */
    public boolean canAddProduct(Plan plan, int currentProductCount) {
        if (plan.getMaxProducts() == null) {
            return true; // Ilimitado
        }
        return currentProductCount < plan.getMaxProducts();
    }
    
    /**
     * Valida se uma organização pode ser adicionada ao plano
     */
    public boolean canAddOrganization(Plan plan, int currentOrgCount) {
        if (plan.getMaxOrganizations() == null) {
            return true; // Ilimitado
        }
        return currentOrgCount < plan.getMaxOrganizations();
    }
    
    /**
     * Verifica se o plano tem acesso a relatórios
     */
    public boolean hasReportsAccess(Plan plan) {
        return plan.getHasReports() != null && plan.getHasReports();
    }
    
    /**
     * Verifica se o plano tem acesso a analytics avançado
     */
    public boolean hasAdvancedAnalyticsAccess(Plan plan) {
        return plan.getHasAdvancedAnalytics() != null && plan.getHasAdvancedAnalytics();
    }
    
    /**
     * Verifica se o plano tem acesso à API
     */
    public boolean hasApiAccess(Plan plan) {
        return plan.getHasApiAccess() != null && plan.getHasApiAccess();
    }
    
    /**
     * Retorna informações detalhadas sobre os recursos do plano
     */
    public Map<String, Object> getPlanFeatures(Plan plan) {
        Map<String, Object> features = new HashMap<>();
        
        features.put("name", plan.getName());
        features.put("description", plan.getDescription());
        features.put("price", plan.getPrice());
        features.put("type", plan.getType());
        
        // Limites
        features.put("maxUsers", plan.getMaxUsers() != null ? plan.getMaxUsers() : "Ilimitado");
        features.put("maxProducts", plan.getMaxProducts() != null ? plan.getMaxProducts() : "Ilimitado");
        features.put("maxOrganizations", plan.getMaxOrganizations() != null ? plan.getMaxOrganizations() : "Ilimitado");
        
        // Recursos
        features.put("hasReports", hasReportsAccess(plan));
        features.put("hasAdvancedAnalytics", hasAdvancedAnalyticsAccess(plan));
        features.put("hasApiAccess", hasApiAccess(plan));
        
        // Integração Stripe
        features.put("stripeProductId", plan.getStripeProductId());
        features.put("stripePriceId", plan.getStripePriceId());
        
        return features;
    }
}