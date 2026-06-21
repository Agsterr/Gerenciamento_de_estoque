package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

public record UsuarioCreatedResponse(
        UsuarioDto usuario,
        String temporaryPassword
) {}
