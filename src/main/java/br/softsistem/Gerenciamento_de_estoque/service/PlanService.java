package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    /**
     * Retorna planos ativos ordenados por preço (ASC)
     */
    public List<Plan> getActivePlansOrderedByPrice() {
        return planRepository.findActivePlansOrderByPrice();
    }

    /**
     * Busca um plano ativo por ID (filtra por isActive = true)
     */
    public Optional<Plan> getActivePlanById(Long id) {
        return planRepository.findById(id)
                .filter(plan -> Boolean.TRUE.equals(plan.getIsActive()));
    }
}