package br.softsistem.Gerenciamento_de_estoque.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.service.MercadoPagoPlanService;
import br.softsistem.Gerenciamento_de_estoque.service.PlanService;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);

    private final PlanService planService;
    private final MercadoPagoPlanService mercadoPagoPlanService;

    public PlanController(
            PlanService planService,
            @Autowired(required = false) MercadoPagoPlanService mercadoPagoPlanService) {
        this.planService = planService;
        this.mercadoPagoPlanService = mercadoPagoPlanService;
    }

    @GetMapping
    public ResponseEntity<List<Plan>> getAllPlans() {
        List<Plan> plans = planService.findAllActivePlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * Plano padrão do SaaS (mensalidade do Gerenciamento de Estoque).
     * O front usa este ID no checkout Asaas após o trial.
     */
    @GetMapping("/default")
    public ResponseEntity<Plan> getDefaultSaasPlan() {
        return planService.getDefaultSaasPlan()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Garante que um plano do Mercado Pago exista no banco local.
     * Mapeamento explícito antes de /{id} para evitar que "ensure-from-mp" seja interpretado como id.
     */
    @PostMapping("/ensure-from-mp")
    public ResponseEntity<Plan> ensureFromMercadoPago(@RequestBody Map<String, String> body) {
        String mpPlanId = body != null ? body.get("mpPlanId") : null;
        if (mpPlanId == null || mpPlanId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Plan plan = planService.ensureFromMercadoPago(mpPlanId.trim());
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/mp-plans")
    public ResponseEntity<List<Map<String, Object>>> getMercadoPagoPlans() {
        if (mercadoPagoPlanService == null) {
            return ResponseEntity.ok(List.of());
        }
        try {
            List<Map<String, Object>> plans = mercadoPagoPlanService.getPlans();
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("Erro ao buscar planos do Mercado Pago", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("error", e.getMessage())));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Plan> getPlanById(@PathVariable Long id) {
        return planService.getPlanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Plan> getPlanByType(@PathVariable PlanType type) {
        return planService.getPlanByType(type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/features")
    public ResponseEntity<Map<String, Object>> getPlanFeatures(@PathVariable Long id) {
        return planService.getPlanFeaturesById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/validate-user")
    public ResponseEntity<Map<String, Object>> validateUserAddition(
            @PathVariable Long id,
            @RequestParam int currentUserCount) {
        return planService.validateUserAddition(id, currentUserCount)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/validate-product")
    public ResponseEntity<Map<String, Object>> validateProductAddition(
            @PathVariable Long id,
            @RequestParam int currentProductCount) {
        return planService.validateProductAddition(id, currentProductCount)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Plan> createPlan(@Valid @RequestBody Plan plan) {
        Plan createdPlan = planService.createPlan(plan);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlan);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Plan> updatePlan(@PathVariable Long id, @Valid @RequestBody Plan plan) {
        Plan updatedPlan = planService.updatePlan(id, plan);
        return ResponseEntity.ok(updatedPlan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate-for-checkout")
    public ResponseEntity<Map<String, String>> validateForCheckout() {
        Map<String, String> result = planService.performMercadoPagoSync();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync-mercadopago")
    public ResponseEntity<Map<String, Object>> syncMercadoPago(
            @RequestParam(name = "deactivateMissing", required = false, defaultValue = "true") boolean deactivateMissing) {
        Map<String, Object> result = planService.syncMercadoPagoPlans(deactivateMissing);
        return ResponseEntity.ok(result);
    }

    }
