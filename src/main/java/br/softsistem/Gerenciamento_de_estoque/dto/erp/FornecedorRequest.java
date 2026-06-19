package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.NotBlank;

public record FornecedorRequest(
        @NotBlank String nome,
        String cnpj,
        String email,
        String telefone,
        String endereco
) {}
