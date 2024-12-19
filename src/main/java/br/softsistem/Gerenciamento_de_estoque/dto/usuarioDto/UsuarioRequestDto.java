package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UsuarioRequestDto(
        @NotBlank(message = "O username não pode ser vazio.") String username,
        @NotBlank(message = "A senha não pode ser vazia.") String senha,
        @Email(message = "Email inválido.") String email,
        List<String> roles
) {
}


