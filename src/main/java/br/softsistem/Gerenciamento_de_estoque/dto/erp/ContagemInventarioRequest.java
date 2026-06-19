package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.NotNull;

public record ContagemInventarioRequest(
        @NotNull Long depositoId,
        String observacao
) {}
