package br.softsistem.Gerenciamento_de_estoque.dto.planDto;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import java.math.BigDecimal;

public record PlanSummaryDto(
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
        Boolean hasApiAccess
) {}