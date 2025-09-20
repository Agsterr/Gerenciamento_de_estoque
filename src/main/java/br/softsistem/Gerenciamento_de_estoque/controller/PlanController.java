package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para gerenciamento de planos de assinatura
 */
@RestController
@RequestMapping("/api/plans")
@Tag(name = "Planos", description = "Gerenciamento de planos de assinatura")
public class PlanController {
    
    private final PlanService planService;
    
    public PlanController(PlanService planService) {
        this.planService = planService;
    }
    
    /**
     * Lista todos os planos ativos
     */
    @GetMapping
    @Operation(
        summary = "Listar planos ativos",
        description = "Retorna todos os planos de assinatura ativos disponíveis"
    )
    @ApiResponse(responseCode = "200", description = "Lista de planos retornada com sucesso")
    public ResponseEntity<List<Plan>> getAllActivePlans() {
        List<Plan> plans = planService.findAllActivePlans();
        return ResponseEntity.ok(plans);
    }
    
    /**
     * Busca plano por ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Buscar plano por ID",
        description = "Retorna um plano específico pelo seu ID"
    )
    @ApiResponse(responseCode = "200", description = "Plano encontrado")
    @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    public ResponseEntity<Plan> getPlanById(
            @Parameter(description = "ID do plano") @PathVariable Long id) {
        Optional<Plan> plan = planService.findById(id);
        return plan.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Busca plano por tipo
     */
    @GetMapping("/type/{type}")
    @Operation(
        summary = "Buscar plano por tipo",
        description = "Retorna um plano específico pelo seu tipo (BASIC, PROFESSIONAL, ENTERPRISE)"
    )
    @ApiResponse(responseCode = "200", description = "Plano encontrado")
    @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    public ResponseEntity<Plan> getPlanByType(
            @Parameter(description = "Tipo do plano") @PathVariable PlanType type) {
        Optional<Plan> plan = planService.findByType(type);
        return plan.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Retorna recursos detalhados de um plano
     */
    @GetMapping("/{id}/features")
    @Operation(
        summary = "Recursos do plano",
        description = "Retorna informações detalhadas sobre os recursos e limites de um plano"
    )
    @ApiResponse(responseCode = "200", description = "Recursos do plano retornados")
    @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    public ResponseEntity<Map<String, Object>> getPlanFeatures(
            @Parameter(description = "ID do plano") @PathVariable Long id) {
        Optional<Plan> plan = planService.findById(id);
        if (plan.isPresent()) {
            Map<String, Object> features = planService.getPlanFeatures(plan.get());
            return ResponseEntity.ok(features);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Valida se é possível adicionar um usuário ao plano
     */
    @GetMapping("/{id}/validate/user")
    @Operation(
        summary = "Validar adição de usuário",
        description = "Verifica se é possível adicionar mais um usuário ao plano baseado nos limites"
    )
    @ApiResponse(responseCode = "200", description = "Validação realizada")
    @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    public ResponseEntity<Map<String, Object>> validateAddUser(
            @Parameter(description = "ID do plano") @PathVariable Long id,
            @Parameter(description = "Número atual de usuários") @RequestParam int currentUserCount) {
        Optional<Plan> plan = planService.findById(id);
        if (plan.isPresent()) {
            boolean canAdd = planService.canAddUser(plan.get(), currentUserCount);
            Map<String, Object> result = Map.of(
                "canAddUser", canAdd,
                "currentCount", currentUserCount,
                "maxUsers", plan.get().getMaxUsers() != null ? plan.get().getMaxUsers() : "Ilimitado"
            );
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Valida se é possível adicionar um produto ao plano
     */
    @GetMapping("/{id}/validate/product")
    @Operation(
        summary = "Validar adição de produto",
        description = "Verifica se é possível adicionar mais um produto ao plano baseado nos limites"
    )
    @ApiResponse(responseCode = "200", description = "Validação realizada")
    @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    public ResponseEntity<Map<String, Object>> validateAddProduct(
            @Parameter(description = "ID do plano") @PathVariable Long id,
            @Parameter(description = "Número atual de produtos") @RequestParam int currentProductCount) {
        Optional<Plan> plan = planService.findById(id);
        if (plan.isPresent()) {
            boolean canAdd = planService.canAddProduct(plan.get(), currentProductCount);
            Map<String, Object> result = Map.of(
                "canAddProduct", canAdd,
                "currentCount", currentProductCount,
                "maxProducts", plan.get().getMaxProducts() != null ? plan.get().getMaxProducts() : "Ilimitado"
            );
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Valida se é possível adicionar uma organização ao plano
     */
    @GetMapping("/{id}/validate/organization")
    @Operation(
        summary = "Validar adição de organização",
        description = "Verifica se é possível adicionar mais uma organização ao plano baseado nos limites"
    )
    @ApiResponse(responseCode = "200", description = "Validação realizada")
    @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    public ResponseEntity<Map<String, Object>> validateAddOrganization(
            @Parameter(description = "ID do plano") @PathVariable Long id,
            @Parameter(description = "Número atual de organizações") @RequestParam int currentOrgCount) {
        Optional<Plan> plan = planService.findById(id);
        if (plan.isPresent()) {
            boolean canAdd = planService.canAddOrganization(plan.get(), currentOrgCount);
            Map<String, Object> result = Map.of(
                "canAddOrganization", canAdd,
                "currentCount", currentOrgCount,
                "maxOrganizations", plan.get().getMaxOrganizations() != null ? plan.get().getMaxOrganizations() : "Ilimitado"
            );
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Sincroniza planos com Stripe (endpoint administrativo)
     */
    @PostMapping("/sync-stripe")
    @Operation(
        summary = "Sincronizar com Stripe",
        description = "Sincroniza todos os planos com o Stripe, criando produtos e preços automaticamente"
    )
    @ApiResponse(responseCode = "200", description = "Sincronização realizada com sucesso")
    @ApiResponse(responseCode = "500", description = "Erro durante a sincronização")
    public ResponseEntity<Map<String, String>> syncWithStripe() {
        try {
            planService.syncAllPlansWithStripe();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Planos sincronizados com Stripe com sucesso"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Erro ao sincronizar com Stripe: " + e.getMessage()
            ));
        }
    }
}