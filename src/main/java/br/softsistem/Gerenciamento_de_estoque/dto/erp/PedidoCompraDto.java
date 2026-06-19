package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusPedidoCompra;
import br.softsistem.Gerenciamento_de_estoque.model.PedidoCompra;
import br.softsistem.Gerenciamento_de_estoque.model.PedidoCompraItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoCompraDto(
        Long id, String numero, Long fornecedorId, String fornecedorNome,
        Long depositoId, String depositoNome, StatusPedidoCompra status,
        String observacao, BigDecimal valorTotal, Long orgId,
        LocalDateTime criadoEm, List<PedidoCompraItemDto> itens
) {
    public PedidoCompraDto(PedidoCompra p) {
        this(p.getId(), p.getNumero(),
                p.getFornecedor().getId(), p.getFornecedor().getNome(),
                p.getDeposito() != null ? p.getDeposito().getId() : null,
                p.getDeposito() != null ? p.getDeposito().getNome() : null,
                p.getStatus(), p.getObservacao(), p.getValorTotal(),
                p.getOrg().getId(), p.getCriadoEm(),
                p.getItens().stream().map(i -> new PedidoCompraItemDto(
                        i.getId(), i.getProduto().getId(), i.getProduto().getNome(),
                        i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal())).toList());
    }
}
