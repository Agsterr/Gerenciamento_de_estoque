package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

import jakarta.validation.constraints.NotBlank;

public record CreateUsuarioOrgRequest(
        @NotBlank(message = "O nome de usuário é obrigatório.") String username,
        String email
) {}
