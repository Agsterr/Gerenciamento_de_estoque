package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioPasswordResponse;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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

    @Transactional
    public UsuarioPasswordResponse resetSenha(Long id, Long orgId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado ou não pertence à organização"));
        if (usuario.isSuperAdmin()) {
            throw new IllegalArgumentException("Não é permitido resetar senha de SUPER_ADMIN.");
        }
        String tempPassword = generateTemporaryPassword();
        usuario.setSenha(passwordEncoder.encode(tempPassword));
        usuarioRepository.save(usuario);
        return new UsuarioPasswordResponse(usuario.getUsername(), tempPassword);
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
