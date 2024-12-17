package br.softsistem.Gerenciamento_de_estoque.dto;

import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;

public record UsuarioDto(String username, String email, Boolean ativo, java.util.List<Role> role, Long id) {


    public UsuarioDto(Usuario usuario) {
        this(usuario.getUsername(),usuario.getEmail(),usuario. getAtivo(),usuario.getRoles(),usuario.getId()); } }
