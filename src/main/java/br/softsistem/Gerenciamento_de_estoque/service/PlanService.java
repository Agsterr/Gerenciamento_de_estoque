package br.softsistem.Gerenciamento_de_estoque.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;

/**
 * Service para gerenciamento de Planos
 * REFATORADO: Melhor integração com MercadoPago services corrigidos
 */
@Service
@Transactional
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);

    private final PlanRepository planRepository;
    private final MercadoPagoConfig mercadoPagoConfig;
    private final MercadoPagoPlanService mercadoPagoPlanService;

    public PlanService(
            PlanRepository planRepository,
            MercadoPagoConfig mercadoPagoConfig,
            @Autowired(required = false) MercadoPagoPlanService mercadoPagoPlanService
    ) {
        this.planRepository = planRepository;
        this.mercadoPagoConfig = mercadoPagoConfig;
        this.mercadoPagoPlanService = mercadoPagoPlanService;
    }

    // ========================================================================
    // MÉTODOS DE CONSULTA
    // ========================================================================

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
     * Plano mensal padrão do SaaS (Gerenciamento de Estoque — R$ 69,90).
     */
    public Optional<Plan> getDefaultSaasPlan() {
        return planRepository.findByType(PlanType.BASIC)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()));
    }

    /**
     * Busca plano ativo pelo ID do Mercado Pago
     */
    public Optional<Plan> findActivePlanByMercadoPagoPlanId(String mpPlanId) {
        return planRepository.findByMercadoPagoPreapprovalPlanIdAndIsActiveTrue(mpPlanId);
    }

    /**
     * Retorna informações detalhadas sobre os recursos do plano
     */
    public Map<String, Object> getPlanFeatures(Plan plan) {
        Map<String, Object> features = new HashMap<>();

        features.put("id", plan.getId());
        features.put("name", plan.getName());
        features.put("description", plan.getDescription());
        features.put("price", plan.getPrice());
        features.put("type", plan.getType());

        // Limites
        features.put("maxUsers", plan.getMaxUsers() != null ? plan.getMaxUsers() : "Ilimitado");
        features.put("maxProducts", plan.getMaxProducts() != null ? plan.getMaxProducts() : "Ilimitado");

        // Recursos
        features.put("hasReports", hasReportsAccess(plan));
        features.put("hasAdvancedAnalytics", hasAdvancedAnalyticsAccess(plan));
        features.put("hasApiAccess", hasApiAccess(plan));

        // Integração Mercado Pago
        features.put("mercadoPagoPreapprovalPlanId", plan.getMercadoPagoPreapprovalPlanId());
        features.put("mercadoPagoFrequency", plan.getMercadoPagoFrequency());
        features.put("mercadoPagoReady", mercadoPagoConfig.isMercadoPagoConfigured());

        return features;
    }

    // ========================================================================
    // SINCRONIZAÇÃO COM MERCADO PAGO
    // ========================================================================

    /**
     * ⭐ MÉTODO PRINCIPAL DE SINCRONIZAÇÃO ⭐
     * Sincroniza planos do Mercado Pago com o banco de dados local
     *
     * @param deactivateMissing Se true, desativa planos locais que não existem mais no MP
     * @return Estatísticas da sincronização
     */
    @Transactional
    public Map<String, Object> syncMercadoPagoPlans(boolean deactivateMissing) {
        log.info("🔄 Iniciando sincronização de planos com Mercado Pago...");

        if (!mercadoPagoConfig.isMercadoPagoConfigured()) {
            log.warn("⚠️ Mercado Pago não está configurado!");
            return Map.of(
                    "status", "error",
                    "message", "Mercado Pago não configurado"
            );
        }
        if (mercadoPagoPlanService == null) {
            return Map.of("status", "error", "message", "Mercado Pago não habilitado (app.payment.provider != mercadopago)");
        }

        List<Map<String, Object>> mpPlans = mercadoPagoPlanService.getActivePlans();

        int created = 0;
        int updated = 0;
        int deactivated = 0;
        java.util.Set<String> currentMpIds = new java.util.HashSet<>();

        // Processa cada plano do Mercado Pago
        for (Map<String, Object> mpPlan : mpPlans) {
            String mpId = extractString(mpPlan, "id");

            if (mpId == null || mpId.isBlank()) {
                log.warn("⚠️ Plano sem ID, ignorando...");
                continue;
            }

            currentMpIds.add(mpId);

            String name = extractString(mpPlan, "name");
            BigDecimal price = parsePrice(mpPlan.get("price"));
            Integer frequency = parseFrequency(mpPlan.get("frequency"));
            String status = extractString(mpPlan, "status");

            // ⭐ Verifica se plano tem free trial configurado
            Boolean hasFreeTrial = (Boolean) mpPlan.get("has_free_trial");
            Integer freeTrialDays = null;

            if (Boolean.TRUE.equals(hasFreeTrial)) {
                freeTrialDays = parseFrequency(mpPlan.get("free_trial_frequency"));
                log.info("  🎁 Plano com free trial: {} dias", freeTrialDays);
            }

            boolean isActive = "active".equalsIgnoreCase(status);

            Optional<Plan> existingOpt = planRepository.findByMercadoPagoPreapprovalPlanId(mpId);

            if (existingOpt.isPresent()) {
                // ⭐ ATUALIZA PLANO EXISTENTE
                Plan existing = existingOpt.get();
                boolean changed = false;

                if (name != null && !name.equals(existing.getName())) {
                    existing.setName(name);
                    changed = true;
                }

                if (price != null && (existing.getPrice() == null || existing.getPrice().compareTo(price) != 0)) {
                    existing.setPrice(price);
                    changed = true;
                }

                if (frequency != null && !java.util.Objects.equals(existing.getMercadoPagoFrequency(), frequency)) {
                    existing.setMercadoPagoFrequency(frequency);
                    changed = true;
                }

                if (Boolean.TRUE.equals(existing.getIsActive()) != isActive) {
                    existing.setIsActive(isActive);
                    changed = true;
                }

                if (changed) {
                    planRepository.save(existing);
                    updated++;
                    log.info("✅ Plano atualizado: {} (R$ {})", existing.getName(), existing.getPrice());
                }

            } else {
                // ⭐ CRIA NOVO PLANO
                if (name == null || price == null) {
                    log.warn("⚠️ Plano com dados insuficientes: mpId={} name={} price={}", mpId, name, price);
                    continue;
                }

                Plan newPlan = new Plan();
                newPlan.setName(name);
                newPlan.setDescription(buildDescription(mpPlan, hasFreeTrial, freeTrialDays));
                newPlan.setPrice(price);
                newPlan.setType(PlanType.BASIC); // Padrão
                newPlan.setMercadoPagoPreapprovalPlanId(mpId);
                newPlan.setMercadoPagoFrequency(frequency);
                newPlan.setIsActive(isActive);

                planRepository.save(newPlan);
                created++;
                log.info("✅ Plano criado: {} (R$ {})", name, price);
            }
        }

        // ⭐ DESATIVA PLANOS QUE NÃO EXISTEM MAIS NO MP
        if (deactivateMissing) {
            List<Plan> linkedPlans = planRepository.findByMercadoPagoPreapprovalPlanIdIsNotNull();

            for (Plan p : linkedPlans) {
                String mpId = p.getMercadoPagoPreapprovalPlanId();

                if (mpId != null && !currentMpIds.contains(mpId) && Boolean.TRUE.equals(p.getIsActive())) {
                    p.setIsActive(false);
                    planRepository.save(p);
                    deactivated++;
                    log.info("❌ Plano desativado (ausente no MP): {}", p.getName());
                }
            }
        }

        // Resultado da sincronização
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("created", created);
        result.put("updated", updated);
        result.put("deactivated", deactivated);
        result.put("total_api", mpPlans.size());

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ Sincronização concluída:");
        log.info("  📝 Criados: {}", created);
        log.info("  🔄 Atualizados: {}", updated);
        log.info("  ❌ Desativados: {}", deactivated);
        log.info("  📦 Total no MP: {}", mpPlans.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return result;
    }

    /**
     * Garante que um plano do Mercado Pago exista no banco local.
     * Se já existir (por mercadoPagoPreapprovalPlanId), retorna o existente.
     * Se não existir, busca no MP, cria no banco e retorna.
     * Usado pelo front quando exibe planos vindos de GET /api/plans/mp-plans e o usuário escolhe assinar.
     *
     * @param mpPlanId ID do plano no Mercado Pago (preapproval_plan_id)
     * @return Plano local (existente ou recém-criado)
     */
    @Transactional
    public Plan ensureFromMercadoPago(String mpPlanId) {
        if (mpPlanId == null || mpPlanId.isBlank()) {
            throw new IllegalArgumentException("mpPlanId é obrigatório");
        }
        Optional<Plan> existing = planRepository.findByMercadoPagoPreapprovalPlanIdAndIsActiveTrue(mpPlanId);
        if (existing.isPresent()) {
            log.debug("Plano já existe no banco: {}", existing.get().getId());
            return existing.get();
        }
        if (!mercadoPagoConfig.isMercadoPagoConfigured()) {
            throw new IllegalStateException("Mercado Pago não está configurado");
        }
        if (mercadoPagoPlanService == null) {
            throw new IllegalStateException("Mercado Pago não habilitado");
        }
        Map<String, Object> mpPlan = mercadoPagoPlanService.getPlanById(mpPlanId);
        if (mpPlan == null) {
            throw new IllegalArgumentException("Plano não encontrado no Mercado Pago: " + mpPlanId);
        }
        String name = extractString(mpPlan, "name");
        BigDecimal price = parsePrice(mpPlan.get("price"));
        if (name == null || price == null) {
            throw new IllegalArgumentException("Plano do Mercado Pago sem nome ou preço válido");
        }
        Integer frequency = parseFrequency(mpPlan.get("frequency"));
        String status = extractString(mpPlan, "status");
        Boolean hasFreeTrial = (Boolean) mpPlan.get("has_free_trial");
        Integer freeTrialDays = Boolean.TRUE.equals(hasFreeTrial) ? parseFrequency(mpPlan.get("free_trial_frequency")) : null;
        boolean isActive = "active".equalsIgnoreCase(status);

        Plan newPlan = new Plan();
        newPlan.setName(name);
        newPlan.setDescription(buildDescription(mpPlan, hasFreeTrial, freeTrialDays));
        newPlan.setPrice(price);
        newPlan.setType(PlanType.BASIC);
        newPlan.setMercadoPagoPreapprovalPlanId(mpPlanId);
        newPlan.setMercadoPagoFrequency(frequency);
        newPlan.setIsActive(isActive);
        setDefaultValues(newPlan);
        Plan saved = planRepository.save(newPlan);
        log.info("✅ Plano criado a partir do MP: id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * Valida todos os planos ativos para uso com assinaturas
     */
    public void validatePlansForCheckoutPro() {
        if (!mercadoPagoConfig.isMercadoPagoConfigured()) {
            log.warn("⚠️ Mercado Pago não está configurado!");
            return;
        }

        List<Plan> plans = planRepository.findByIsActiveTrue();
        log.info("🔍 Validando {} planos ativos...", plans.size());

        int valid = 0;
        int invalid = 0;

        for (Plan plan : plans) {
            try {
                validatePlanForMercadoPago(plan);

                // ⭐ Valida se o plano existe e está ativo no Mercado Pago
                if (plan.getMercadoPagoPreapprovalPlanId() != null) {
                    boolean isActive = mercadoPagoPlanService.isPlanActive(plan.getMercadoPagoPreapprovalPlanId());

                    if (!isActive) {
                        log.warn("⚠️ Plano '{}' está inativo no Mercado Pago!", plan.getName());
                        invalid++;
                    } else {
                        log.info("✅ Plano '{}' validado", plan.getName());
                        valid++;
                    }
                } else {
                    log.warn("⚠️ Plano '{}' sem ID do Mercado Pago", plan.getName());
                    invalid++;
                }

            } catch (IllegalStateException e) {
                log.error("❌ Plano '{}' inválido: {}", plan.getName(), e.getMessage());
                invalid++;
            } catch (Exception e) {
                log.error("❌ Erro ao validar plano '{}': {}", plan.getName(), e.getMessage());
                invalid++;
            }
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✅ Validação concluída: {} válidos, {} inválidos", valid, invalid);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Método legado (compatibilidade)
     */
    @Deprecated
    public void syncAllPlansWithMercadoPago() {
        log.warn("⚠️ syncAllPlansWithMercadoPago() está deprecated. Use syncMercadoPagoPlans()");
        validatePlansForCheckoutPro();
    }

    /**
     * Método legado (compatibilidade)
     */
    @Deprecated
    public Map<String, String> performMercadoPagoSync() {
        log.warn("⚠️ performMercadoPagoSync() está deprecated. Use syncMercadoPagoPlans()");

        try {
            Map<String, Object> result = syncMercadoPagoPlans(true);
            return Map.of(
                    "status", "success",
                    "message", "Sincronização concluída: " + result
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "Erro: " + e.getMessage()
            );
        }
    }

    // ========================================================================
    // CRUD DE PLANOS
    // ========================================================================

    /**
     * Cria um novo plano
     */
    public Plan createPlan(Plan plan) {
        log.info("📝 Criando novo plano: {}", plan.getName());

        // Validações
        validatePlanData(plan);

        // Verificar duplicidade
        if (planRepository.existsByName(plan.getName())) {
            throw new IllegalArgumentException("Já existe um plano com este nome");
        }

        // Valores padrão
        setDefaultValues(plan);

        Plan savedPlan = planRepository.save(plan);
        log.info("✅ Plano criado: ID={} Nome={}", savedPlan.getId(), savedPlan.getName());

        return savedPlan;
    }

    /**
     * Atualiza um plano existente
     */
    public Plan updatePlan(Long id, Plan planData) {
        log.info("🔄 Atualizando plano ID: {}", id);

        Plan existingPlan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + id));

        // Atualiza campos
        updatePlanFields(existingPlan, planData);

        Plan updatedPlan = planRepository.save(existingPlan);
        log.info("✅ Plano atualizado: ID={}", updatedPlan.getId());

        return updatedPlan;
    }

    /**
     * Deleta ou desativa um plano
     */
    public void deletePlan(Long id) {
        log.info("🗑️ Deletando/desativando plano ID: {}", id);

        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + id));

        // Se tem assinaturas, apenas desativa
        if (plan.getSubscriptions() != null && !plan.getSubscriptions().isEmpty()) {
            plan.setIsActive(false);
            planRepository.save(plan);
            log.info("✅ Plano desativado (há assinaturas): ID={}", id);
        } else {
            planRepository.delete(plan);
            log.info("✅ Plano deletado: ID={}", id);
        }
    }

    // ========================================================================
    // VALIDAÇÕES E REGRAS DE NEGÓCIO
    // ========================================================================

    /**
     * Valida se um plano está pronto para Mercado Pago
     */
    private void validatePlanForMercadoPago(Plan plan) {
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Preço deve ser maior que zero");
        }

        if (plan.getName() == null || plan.getName().trim().isEmpty()) {
            throw new IllegalStateException("Nome é obrigatório");
        }
    }

    /**
     * Valida dados do plano
     */
    private void validatePlanData(Plan plan) {
        if (plan.getName() == null || plan.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do plano é obrigatório");
        }

        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço deve ser maior que zero");
        }

        if (plan.getType() == null) {
            throw new IllegalArgumentException("Tipo do plano é obrigatório");
        }
    }

    /**
     * Define valores padrão para um novo plano
     */
    private void setDefaultValues(Plan plan) {
        if (plan.getIsActive() == null) plan.setIsActive(true);
        if (plan.getHasReports() == null) plan.setHasReports(false);
        if (plan.getHasAdvancedAnalytics() == null) plan.setHasAdvancedAnalytics(false);
        if (plan.getHasApiAccess() == null) plan.setHasApiAccess(false);
    }

    /**
     * Atualiza campos de um plano
     */
    private void updatePlanFields(Plan existing, Plan data) {
        if (data.getName() != null && !data.getName().trim().isEmpty()) {
            if (!existing.getName().equals(data.getName()) && planRepository.existsByName(data.getName())) {
                throw new IllegalArgumentException("Já existe outro plano com este nome");
            }
            existing.setName(data.getName());
        }

        if (data.getDescription() != null) existing.setDescription(data.getDescription());
        if (data.getPrice() != null && data.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            existing.setPrice(data.getPrice());
        }
        if (data.getType() != null) existing.setType(data.getType());
        if (data.getMaxUsers() != null) existing.setMaxUsers(data.getMaxUsers());
        if (data.getMaxProducts() != null) existing.setMaxProducts(data.getMaxProducts());
        if (data.getHasReports() != null) existing.setHasReports(data.getHasReports());
        if (data.getHasAdvancedAnalytics() != null) existing.setHasAdvancedAnalytics(data.getHasAdvancedAnalytics());
        if (data.getHasApiAccess() != null) existing.setHasApiAccess(data.getHasApiAccess());
        if (data.getIsActive() != null) existing.setIsActive(data.getIsActive());
    }

    // ========================================================================
    // VERIFICAÇÕES DE RECURSOS
    // ========================================================================

    public boolean canAddUser(Plan plan, int currentUserCount) {
        return plan.getMaxUsers() == null || currentUserCount < plan.getMaxUsers();
    }

    public boolean canAddProduct(Plan plan, int currentProductCount) {
        return plan.getMaxProducts() == null || currentProductCount < plan.getMaxProducts();
    }

    public boolean hasReportsAccess(Plan plan) {
        return Boolean.TRUE.equals(plan.getHasReports());
    }

    public boolean hasAdvancedAnalyticsAccess(Plan plan) {
        return Boolean.TRUE.equals(plan.getHasAdvancedAnalytics());
    }

    public boolean hasApiAccess(Plan plan) {
        return Boolean.TRUE.equals(plan.getHasApiAccess());
    }

    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private BigDecimal parsePrice(Object priceObj) {
        if (priceObj == null) return null;
        if (priceObj instanceof BigDecimal) return (BigDecimal) priceObj;
        if (priceObj instanceof Number) return BigDecimal.valueOf(((Number) priceObj).doubleValue());
        if (priceObj instanceof String) {
            try {
                return new BigDecimal((String) priceObj);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Integer parseFrequency(Object freqObj) {
        if (freqObj == null) return null;
        if (freqObj instanceof Number) return ((Number) freqObj).intValue();
        if (freqObj instanceof String) {
            try {
                return Integer.parseInt((String) freqObj);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String buildDescription(Map<String, Object> mpPlan, Boolean hasFreeTrial, Integer freeTrialDays) {
        StringBuilder desc = new StringBuilder();
        desc.append("Plano sincronizado do Mercado Pago");

        if (Boolean.TRUE.equals(hasFreeTrial) && freeTrialDays != null) {
            desc.append(" - ").append(freeTrialDays).append(" dias grátis");
        }

        return desc.toString();
    }

    // Métodos públicos adicionais (compatibilidade)
    public Optional<Plan> getPlanById(Long id) { return findById(id); }
    public Optional<Plan> getPlanByType(PlanType type) { return findByType(type); }
    public Optional<Map<String, Object>> getPlanFeaturesById(Long id) {
        return findById(id).map(this::getPlanFeatures);
    }

    public Optional<Map<String, Object>> validateUserAddition(Long planId, int currentUserCount) {
        return findById(planId).map(plan -> Map.of(
                "canAddUser", canAddUser(plan, currentUserCount),
                "currentCount", currentUserCount,
                "maxUsers", (Object) (plan.getMaxUsers() != null ? plan.getMaxUsers() : "Ilimitado")
        ));
    }

    public Optional<Map<String, Object>> validateProductAddition(Long planId, int currentProductCount) {
        return findById(planId).map(plan -> Map.of(
                "canAddProduct", canAddProduct(plan, currentProductCount),
                "currentCount", currentProductCount,
                "maxProducts", (Object) (plan.getMaxProducts() != null ? plan.getMaxProducts() : "Ilimitado")
        ));
    }
}