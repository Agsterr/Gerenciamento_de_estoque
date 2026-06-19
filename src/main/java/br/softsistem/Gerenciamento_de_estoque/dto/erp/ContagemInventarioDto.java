package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusContagemInventario;
import br.softsistem.Gerenciamento_de_estoque.model.ContagemInventario;
import java.time.LocalDateTime;
import java.util.List;

public record ContagemInventarioDto(
        Long id, Long depositoId, String depositoNome, StatusContagemInventario status,
        String observacao, Long orgId, LocalDateTime criadoEm, LocalDateTime finalizadoEm,
        List<ContagemInventarioItemDto> itens
) {
    public ContagemInventarioDto(ContagemInventario c) {
        this(c.getId(), c.getDeposito().getId(), c.getDeposito().getNome(), c.getStatus(),
                c.getObservacao(), c.getOrg().getId(), c.getCriadoEm(), c.getFinalizadoEm(),
                c.getItens().stream().map(i -> new ContagemInventarioItemDto(
                        i.getId(), i.getProduto().getId(), i.getProduto().getNome(),
                        i.getQuantidadeSistema(), i.getQuantidadeContada(), i.getDiferenca())).toList());
    }
}
