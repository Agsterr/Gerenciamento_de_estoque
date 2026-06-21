package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

public record UsuarioPasswordResponse(
        String username,
        String temporaryPassword
) {}
