package br.softsistem.Gerenciamento_de_estoque.dto.login;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "O username não pode ser vazio.") String username,
        @NotBlank(message = "A senha não pode ser vazia.") String senha
) {
}
