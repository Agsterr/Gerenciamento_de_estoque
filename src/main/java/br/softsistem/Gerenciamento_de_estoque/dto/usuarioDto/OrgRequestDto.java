package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrgRequestDto(
        @NotBlank(message = "O nome da organização é obrigatório.")
        @Size(max = 100, message = "O nome da organização deve ter no máximo 100 caracteres.")
        String nome
) {}
