package br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto;

import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import java.util.List;

public record UsuarioDto(
        Long id,
        String username,
        String email,
        Boolean ativo,
        List<String> roles
) {
    public UsuarioDto(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getAtivo(),
                usuario.getRoles().stream().map(Role::getNome).toList()
        );
    }
}

