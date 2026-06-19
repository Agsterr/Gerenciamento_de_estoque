package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import jakarta.validation.constraints.NotNull;

public record BypassSubscriptionRequest(
        @NotNull Boolean bypass
) {}
