package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

public record UsuarioLimiteDto(
        long ativos,
        Integer maximo,
        boolean ilimitado,
        String origemLimite
) {}
