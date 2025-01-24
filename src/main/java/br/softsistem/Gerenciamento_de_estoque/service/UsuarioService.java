package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void ativarUsuario(Long id) {
        usuarioRepository.atualizarAtivo(id, true);
    }

    public void desativarUsuario(Long id) {
        usuarioRepository.atualizarAtivo(id, false);
    }

    public List<Usuario> listarUsuariosAtivos() {
        return usuarioRepository.findByAtivoTrue();


    }
}
