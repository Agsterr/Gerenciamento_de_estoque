package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import java.util.List;

public record AdminUserDto(
        Long id,
        String username,
        String email,
        Boolean ativo,
        Long orgId,
        String orgNome,
        Boolean bypassSubscription,
        List<String> roles,
        String senhaRegistrada
) {}
