package br.softsistem.Gerenciamento_de_estoque.dto.admin;

public record AdminOrgSummaryDto(
        Long orgId,
        String orgNome,
        Boolean orgAtivo,
        Boolean ephemeral,
        Integer maxDispositivos,
        Integer maxUsuarios,
        long totalUsuarios,
        long dispositivosAprovados
) {}
