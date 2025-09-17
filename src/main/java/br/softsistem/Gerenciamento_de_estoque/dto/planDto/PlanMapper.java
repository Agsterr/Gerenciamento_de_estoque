package br.softsistem.Gerenciamento_de_estoque.dto.planDto;

import br.softsistem.Gerenciamento_de_estoque.model.Plan;

public final class PlanMapper {
    private PlanMapper() {}

    public static PlanSummaryDto toSummary(Plan p) {
        return new PlanSummaryDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getType(),
                p.getMaxUsers(),
                p.getMaxProducts(),
                p.getMaxOrganizations(),
                p.getHasReports(),
                p.getHasAdvancedAnalytics(),
                p.getHasApiAccess()
        );
    }

    public static PlanDetailsDto toDetails(Plan p) {
        return new PlanDetailsDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getType(),
                p.getMaxUsers(),
                p.getMaxProducts(),
                p.getMaxOrganizations(),
                p.getHasReports(),
                p.getHasAdvancedAnalytics(),
                p.getHasApiAccess(),
                Boolean.TRUE.equals(p.getIsActive())
        );
    }

    public static PlanPublicDto toPublic(Plan p) {
        return new PlanPublicDto(
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getType(),
                p.getMaxUsers(),
                p.getMaxProducts(),
                p.getMaxOrganizations()
        );
    }
}