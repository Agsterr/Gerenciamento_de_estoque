package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.model.EstoqueDeposito;
import java.math.BigDecimal;

public record EstoqueDepositoDto(
        Long id, Long produtoId, String produtoNome, String sku,
        Long depositoId, String depositoNome, Integer quantidade,
        BigDecimal custoMedio, BigDecimal valorTotal
) {
    public EstoqueDepositoDto(EstoqueDeposito e) {
        this(e.getId(), e.getProduto().getId(), e.getProduto().getNome(),
                e.getProduto().getSku(), e.getDeposito().getId(), e.getDeposito().getNome(),
                e.getQuantidade(), e.getProduto().getCustoMedio(),
                e.getProduto().getCustoMedio() != null
                        ? e.getProduto().getCustoMedio().multiply(BigDecimal.valueOf(e.getQuantidade()))
                        : BigDecimal.ZERO);
    }
}
