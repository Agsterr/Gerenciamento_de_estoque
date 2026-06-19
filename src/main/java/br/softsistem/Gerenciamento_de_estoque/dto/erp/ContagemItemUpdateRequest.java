package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.NotNull;

public record ContagemItemUpdateRequest(
        @NotNull Long itemId,
        @NotNull Integer quantidadeContada
) {}
