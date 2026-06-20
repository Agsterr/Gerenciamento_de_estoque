package br.softsistem.Gerenciamento_de_estoque.dto.auth;

public record AuthPublicConfigDto(
        boolean registrationEnabled,
        boolean demoEnabled,
        String demoUsername
) {}
