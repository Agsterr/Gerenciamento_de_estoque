package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TransferenciaDepositoRequest(
        @NotNull Long depositoOrigemId,
        @NotNull Long depositoDestinoId,
        @NotNull Long produtoId,
        @NotNull @Min(1) Integer quantidade
) {}
