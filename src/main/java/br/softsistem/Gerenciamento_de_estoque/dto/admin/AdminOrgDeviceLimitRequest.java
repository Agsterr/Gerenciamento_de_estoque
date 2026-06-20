package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminOrgDeviceLimitRequest(
        @NotNull @Min(0) Integer maxDispositivos
) {}
