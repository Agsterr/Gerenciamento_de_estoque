package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Ativar usuário pela organização
    public void ativarUsuario(Long id, Long orgId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado ou não pertence à organização"));
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    // Reativar usuário pela organização
    public void reativarUsuario(String username, Long orgId) {
        Usuario usuario = usuarioRepository.findByUsernameAndOrgId(username, orgId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado ou não pertence à organização"));

        if (!usuario.getAtivo()) {
            usuario.setAtivo(true);
            usuarioRepository.save(usuario);
        }
    }

    // Desativar usuário pela organização
    public void desativarUsuario(Long id, Long orgId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado ou não pertence à organização"));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    // Listar usuários ativos pela organização com paginação
    public Page<Usuario> listarUsuariosAtivos(Long orgId, Pageable pageable) {
        return usuarioRepository.findByAtivoTrueAndOrgId(orgId, pageable);
    }
}
