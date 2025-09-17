package br.softsistem.Gerenciamento_de_estoque.dto.planDto;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import java.math.BigDecimal;

public record PlanDetailsDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        PlanType type,
        Integer maxUsers,
        Integer maxProducts,
        Integer maxOrganizations,
        Boolean hasReports,
        Boolean hasAdvancedAnalytics,
        Boolean hasApiAccess,
        Boolean active
) {}