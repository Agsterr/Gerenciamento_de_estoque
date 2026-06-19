package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void ativarUsuario(Long id, Long orgId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado ou não pertence à organização"));
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void reativarUsuario(String username, Long orgId) {
        Usuario usuario = usuarioRepository.findByUsernameAndOrgId(username, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado ou não pertence à organização"));

        if (!usuario.getAtivo()) {
            usuario.setAtivo(true);
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public void desativarUsuario(Long id, Long orgId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado ou não pertence à organização"));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listarUsuariosAtivos(Long orgId, Pageable pageable) {
        return usuarioRepository.findByAtivoTrueAndOrgId(orgId, pageable);
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
}
