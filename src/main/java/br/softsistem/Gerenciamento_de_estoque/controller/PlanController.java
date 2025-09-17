package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.planDto.PlanDetailsDto;
import br.softsistem.Gerenciamento_de_estoque.dto.planDto.PlanPublicDto;
import br.softsistem.Gerenciamento_de_estoque.dto.planDto.PlanSummaryDto;
import br.softsistem.Gerenciamento_de_estoque.dto.planDto.PlanMapper;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gerenciamento de planos de assinatura
 */
@RestController
@RequestMapping("/api/plans")
@Tag(name = "Plans", description = "Gerenciamento de planos de assinatura")
public class PlanController {

    private final PlanService planService;

    @org.springframework.beans.factory.annotation.Autowired
    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /**
     * Lista todos os planos ativos
     */
    @GetMapping
    @Operation(summary = "Listar planos", description = "Retorna todos os planos de assinatura ativos")
    public ResponseEntity<List<PlanSummaryDto>> getAllPlans() {
        List<Plan> plans = planService.getActivePlansOrderedByPrice();
        List<PlanSummaryDto> dtos = plans.stream()
                .map(PlanMapper::toSummary)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Busca um plano específico por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar plano", description = "Retorna um plano específico por ID")
    public ResponseEntity<PlanDetailsDto> getPlanById(@PathVariable Long id) {
        return planService.getActivePlanById(id)
                .map(PlanMapper::toDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retorna informações resumidas dos planos para exibição pública
     */
    @GetMapping("/public")
    @Operation(summary = "Planos públicos", description = "Retorna informações básicas dos planos para página de preços")
    public ResponseEntity<List<PlanPublicDto>> getPublicPlans() {
        List<Plan> plans = planService.getActivePlansOrderedByPrice();
        List<PlanPublicDto> dtos = plans.stream()
                .map(PlanMapper::toPublic)
                .toList();
        return ResponseEntity.ok(dtos);
    }
}