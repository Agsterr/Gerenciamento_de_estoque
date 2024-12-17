package br.softsistem.Gerenciamento_de_estoque.dto;

import java.util.List;

public record UsuarioRequestDto(
        String username,
        String email,
        String senha,
        List<String> roles // Recebe apenas os nomes das roles como String
) {}

