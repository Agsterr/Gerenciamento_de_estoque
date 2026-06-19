package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.CondicaoPagamento;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.FormaPagamento;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusPedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoPedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.model.PedidoVenda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoVendaDto(
        Long id, String numero,
        TipoPedidoVenda tipoPedido,
        Long consumidorId, String consumidorNome,
        Long funcionarioId, String funcionarioNome,
        Long vendedorId, String vendedorNome,
        LocalDateTime dataHora,
        FormaPagamento formaPagamento,
        CondicaoPagamento condicaoPagamento,
        StatusPedidoVenda status,
        String observacao, BigDecimal valorTotal, Long orgId,
        LocalDateTime criadoEm, List<PedidoVendaItemDto> itens
) {
    public PedidoVendaDto(PedidoVenda p) {
        this(p.getId(), p.getNumero(), p.getTipoPedido(),
                p.getConsumidor() != null ? p.getConsumidor().getId() : null,
                p.getConsumidor() != null ? p.getConsumidor().getNome() : null,
                p.getFuncionario() != null ? p.getFuncionario().getId() : null,
                p.getFuncionario() != null ? p.getFuncionario().getUsername() : null,
                p.getVendedor().getId(), p.getVendedor().getUsername(),
                p.getDataHora(), p.getFormaPagamento(), p.getCondicaoPagamento(),
                p.getStatus(), p.getObservacao(), p.getValorTotal(),
                p.getOrg().getId(), p.getCriadoEm(),
                p.getItens().stream().map(i -> new PedidoVendaItemDto(
                        i.getId(), i.getProduto().getId(), i.getProduto().getNome(),
                        i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal())).toList());
    }
}
