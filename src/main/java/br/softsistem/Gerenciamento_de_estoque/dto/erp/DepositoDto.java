package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.model.Deposito;
import java.time.LocalDateTime;

public record DepositoDto(
        Long id, String nome, String endereco, Boolean padrao, Boolean ativo,
        Long orgId, LocalDateTime criadoEm
) {
    public DepositoDto(Deposito d) {
        this(d.getId(), d.getNome(), d.getEndereco(), d.getPadrao(), d.getAtivo(),
                d.getOrg().getId(), d.getCriadoEm());
    }
}
