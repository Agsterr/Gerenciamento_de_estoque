package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PedidoCompraRequest(
        @NotNull Long fornecedorId,
        Long depositoId,
        String observacao,
        List<PedidoCompraItemRequest> itens
) {}
