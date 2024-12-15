package br.softsistem.Gerenciamento_de_estoque.dto;

import br.softsistem.Gerenciamento_de_estoque.model.Role;

public record RoleDTO(Long id, String nome) {
    public static RoleDTO fromEntity(Role role) {
        return new RoleDTO(role.getId(), role.getNome());
    }
}
