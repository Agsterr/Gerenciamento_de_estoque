package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminCreateUserRequest(
        @NotBlank String username,
        String email,
        @NotNull Long orgId,
        List<String> roles,
        Boolean bypassSubscription
) {}
