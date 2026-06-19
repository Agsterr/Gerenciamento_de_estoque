package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.NotBlank;

public record DepositoRequest(
        @NotBlank String nome,
        String endereco,
        Boolean padrao
) {}
