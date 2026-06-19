package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import java.math.BigDecimal;

public record PedidoCompraItemDto(
        Long id, Long produtoId, String produtoNome,
        Integer quantidade, BigDecimal precoUnitario, BigDecimal subtotal
) {}
