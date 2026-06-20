package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SugestaoRequest(
        @NotBlank @Size(min = 5, max = 4000) String texto
) {}
