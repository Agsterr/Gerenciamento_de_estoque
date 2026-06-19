package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PedidoVendaItemRequest(
        @NotNull Long produtoId,
        @NotNull Integer quantidade,
        java.math.BigDecimal precoUnitario
) {}
