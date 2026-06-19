package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PedidoCompraItemRequest(
        @NotNull Long produtoId,
        @NotNull @Min(1) Integer quantidade,
        @NotNull BigDecimal precoUnitario
) {}
