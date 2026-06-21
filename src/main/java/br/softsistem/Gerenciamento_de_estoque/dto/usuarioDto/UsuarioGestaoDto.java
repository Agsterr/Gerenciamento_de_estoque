package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;

import java.util.List;

public record UsuarioGestaoDto(
        Long id,
        String username,
        String email,
        Boolean ativo,
        Long orgId,
        List<String> roles,
        String senhaRegistrada
) {
    public UsuarioGestaoDto(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getAtivo(),
                usuario.getOrg() != null ? usuario.getOrg().getId() : null,
                usuario.getRoles() != null
                        ? usuario.getRoles().stream().map(Role::getNome).toList()
                        : List.of(),
                usuario.getSenhaRegistrada()
        );
    }
}
