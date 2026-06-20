package br.softsistem.Gerenciamento_de_estoque.dto.admin;

public record AdminUserPasswordResponse(
        AdminUserDto user,
        String temporaryPassword
) {}
