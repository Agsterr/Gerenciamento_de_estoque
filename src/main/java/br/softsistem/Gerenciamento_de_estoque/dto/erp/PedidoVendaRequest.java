package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.CondicaoPagamento;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.FormaPagamento;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoPedidoVenda;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoVendaRequest(
        TipoPedidoVenda tipoPedido,
        Long consumidorId,
        Long funcionarioId,
        Long vendedorId,
        LocalDateTime dataHora,
        FormaPagamento formaPagamento,
        CondicaoPagamento condicaoPagamento,
        String observacao,
        Boolean confirmar,
        @NotNull List<PedidoVendaItemRequest> itens
) {}
