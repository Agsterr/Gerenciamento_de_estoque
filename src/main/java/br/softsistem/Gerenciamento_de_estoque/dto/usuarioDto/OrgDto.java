package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

import br.softsistem.Gerenciamento_de_estoque.model.Org;

public record OrgDto(
        Long id,
        String nome,
        Boolean ativo
) {
    public OrgDto(Org org) {
        this(org.getId(), org.getNome(), org.getAtivo());
    }
}
