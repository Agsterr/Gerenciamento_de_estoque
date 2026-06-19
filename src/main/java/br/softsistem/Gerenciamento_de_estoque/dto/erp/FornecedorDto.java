package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.model.Fornecedor;
import java.time.LocalDateTime;

public record FornecedorDto(
        Long id, String nome, String cnpj, String email, String telefone,
        String endereco, Boolean ativo, Long orgId, LocalDateTime criadoEm
) {
    public FornecedorDto(Fornecedor f) {
        this(f.getId(), f.getNome(), f.getCnpj(), f.getEmail(), f.getTelefone(),
                f.getEndereco(), f.getAtivo(), f.getOrg().getId(), f.getCriadoEm());
    }
}
